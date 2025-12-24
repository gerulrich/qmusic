package quantum.music.providers.tdl;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientResponse;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import quantum.music.client.ApiClient;
import quantum.music.domain.providers.Track;
import quantum.music.domain.DecryptionKeys;
import quantum.music.domain.providers.MediaInfo;
import quantum.music.service.TokenService;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import static io.quarkus.arc.ComponentsProvider.LOG;

@ApplicationScoped
public class TdlTrackService extends TldAbstractService  {

    private static final String MEDIA_QUALITY_LOSSLESS = "LOSSLESS";
    private static final String MEDIA_TYPE_STREAM = "STREAM";
    private static final String MEDIA_CONTENT_FULL = "FULL";
    public static final String NONE = "NONE";

    @Inject
    @RestClient
    private ApiClient apiClient;

    @Inject
    TokenService tokenService;

    @Inject
    private Vertx vertx;

    private HttpClient httpClient;
    @ConfigProperty(name = "tdl.master.key")
    private String masterKey;


    @PostConstruct
    void init() {
        httpClient = vertx.createHttpClient();
    }

    public Uni<Track> getTrackById(String trackId) {
        LOG.debugf("Retrieving track details for: %s", trackId);
        return tokenService.withToken(() -> apiClient.track(parsedId(trackId))
                .onItem().transform(json -> new Track(
                        formatId(json.getLong("id")),
                        json.getString("title"),
                        json.getInteger("duration"),
                        json.getInteger("trackNumber"),
                        json.getInteger("volumeNumber"),
                        json.getString("audioQuality"),
                        json.getJsonObject("mediaMetadata").getJsonArray("tags").stream().map(Object::toString).toList(),
                        json.getString("version"),
                        json.getString("copyright"),
                        null,
                        formatResourceUrl("tracks", formatId(json.getLong("id")), "stream")
                ))
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting track: %s", trackId)));
    }

    public Uni<MediaInfo> content(String track, String codec, String quality, String presentation) {
        LOG.debugf("Retrieving media content for track: %s with codec: %s and quality: %s", track, codec, quality);
        String q = quality.replaceAll("HIRES", "HI_RES");
        return tokenService.withToken(() -> apiClient.media(parsedId(track), q, MEDIA_TYPE_STREAM, presentation)
                .onItem().transform(json -> {
                    LOG.debugf("Retrieving content for track: %s", parsedId(track));
                    String manifestMimeType = json.getString("manifestMimeType");
                    if (!manifestMimeType.equals("application/vnd.tidal.bts")) {
                        throw new WebApplicationException("Unsupported manifest type: " + manifestMimeType, 400);
                    }

                    String encodedManifest = json.getString("manifest");
                    JsonObject manifest = new JsonObject(new String(Base64.getDecoder().decode(encodedManifest)));

                    return new MediaInfo(
                            manifest.getJsonArray("urls").getString(0),
                            json.getString("audioQuality"),
                            manifest.getString("codecs"),
                            manifest.getString("encryptionType"),
                            manifest.getString("keyId")
                    );
                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting content for track: %s", track)));
    }

    /**
     * Proxies a file from a given URL.
     *
     * @param mediaInfo Media information containing the URL and encryption type
     * @return A Multi emitting the file's content as Buffer
     */
    public Multi<Buffer> streamFile(MediaInfo mediaInfo) {
        String url = mediaInfo.url();
        String encryption = mediaInfo.encryption();
        if (NONE.equals(encryption)) {
            return Multi.createFrom().emitter(emitter -> {
                httpClient
                        .request(new RequestOptions().setMethod(HttpMethod.GET).setAbsoluteURI(url))
                        .onItem().transformToUni(req -> req.send())
                        .subscribe().with(resp -> handleStreaming(resp, emitter), emitter::fail);
            });
        }
        if ("OLD_AES".equals(encryption)) {
            DecryptionKeys dk = decryptSecurityToken(mediaInfo.keyId());
            LOG.debugf("Decryption keys obtained for streaming, key length: %d, nonce length: %d",
                    dk.key().length, dk.nonce().length);

            return Multi.createFrom().emitter(emitter -> httpClient
                    .request(new RequestOptions().setMethod(HttpMethod.GET).setAbsoluteURI(url))
                    .onItem().transformToUni(req -> req.send())
                    .subscribe().with(resp -> handleDecryptStreaming(dk, resp, emitter), emitter::fail));

        }
        return Multi.createFrom().emitter(emitter ->
                // TODO handle encryption
                emitter.fail(new WebApplicationException("Encryption not supported", 400))
        );
    }

    private void handleStreaming(HttpClientResponse resp, MultiEmitter<? super Buffer> emitter) {
        if (resp.statusCode() != 200) {
            emitter.fail(new WebApplicationException("Failed: " + resp.statusCode(), resp.statusCode()));
            return;
        }

        resp.handler(emitter::emit);
        resp.endHandler(emitter::complete);
        resp.exceptionHandler(emitter::fail);
    }

    private void handleDecryptStreaming(DecryptionKeys dk, HttpClientResponse resp, MultiEmitter<? super Buffer> emitter) {
        if (resp.statusCode() != 200) {
            emitter.fail(new WebApplicationException("Failed: " + resp.statusCode(), resp.statusCode()));
            return;
        }

        if (dk.nonce().length != 8) {
            emitter.fail(new IllegalArgumentException("Nonce must be 8 bytes long."));
            return;
        }

        try {
            // Extend the nonce to 16 bytes by adding 8 bytes of null (0x00)
            byte[] extendedNonce = new byte[16];
            System.arraycopy(dk.nonce(), 0, extendedNonce, 0, 8);
            // Bytes 8-15 are already 0 (default initialization)

            // Initialize AES-CTR cipher
            SecretKeySpec keySpec = new SecretKeySpec(dk.key(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(extendedNonce);
            Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            LOG.debugf("Decrypting stream with AES-CTR");

            // Handle each chunk of encrypted data
            resp.handler(encryptedBuffer -> {
                try {
                    // Decrypt the chunk
                    byte[] encryptedBytes = encryptedBuffer.getBytes();
                    byte[] decryptedBytes = cipher.update(encryptedBytes);

                    if (!emitter.isCancelled() && decryptedBytes != null && decryptedBytes.length > 0) {
                        emitter.emit(Buffer.buffer(decryptedBytes));
                    }
                } catch (Exception e) {
                    if (!emitter.isCancelled()) {
                        LOG.errorf(e, "Error decrypting stream chunk");
                        emitter.fail(e);
                    }
                }
            });

            resp.endHandler(() -> {
                try {
                    // Process final block if any
                    byte[] finalBytes = cipher.doFinal();
                    if (!emitter.isCancelled() && finalBytes != null && finalBytes.length > 0) {
                        emitter.emit(Buffer.buffer(finalBytes));
                    }
                    emitter.complete();

                } catch (Exception e) {
                    if (!emitter.isCancelled()) {
                        LOG.errorf(e, "Error finalizing decryption");
                        emitter.fail(e);
                    }
                }
            });

            resp.exceptionHandler(t -> {
                if (isClientAbort(t)) {
                    LOG.debug("Client aborted audio stream");
                    emitter.complete();
                } else {
                    emitter.fail(t);
                }
            });
        } catch (Exception e) {
            LOG.errorf(e, "Error initializing AES-CTR cipher");
            emitter.fail(e);
        }
    }

    private boolean isClientAbort(Throwable t) {
        return t instanceof java.io.IOException &&
                (t.getMessage() != null && t.getMessage().contains("Broken pipe"));
    }

    /**
     * Decrypts the security token using the master key.
     * This method extracts the decryption key and nonce from an encrypted security token.
     *
     * @param securityToken The base64-encoded security token to decrypt
     * @return The decryption keys containing the key and nonce
     * @throws RuntimeException if decryption fails
     */
    public DecryptionKeys decryptSecurityToken(String securityToken) {
        try {
            // Decode master key and security token from base64
            byte[] masterKeyBytes = Base64.getDecoder().decode(masterKey);
            byte[] securityTokenBytes = Base64.getDecoder().decode(securityToken);

            // Get the IV from the first 16 bytes of the securityToken
            byte[] iv = new byte[16];
            System.arraycopy(securityTokenBytes, 0, iv, 0, 16);

            // Get the encrypted part of the security token (from byte 16 onwards)
            byte[] encryptedSt = new byte[securityTokenBytes.length - 16];
            System.arraycopy(securityTokenBytes, 16, encryptedSt, 0, encryptedSt.length);

            // Initialize AES-256-CBC decryptor
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(masterKeyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            // Decrypt the security token
            byte[] decryptedSt = cipher.doFinal(encryptedSt);

            // Get the audio stream decryption key and nonce from the decrypted security token
            byte[] key = new byte[16];
            byte[] nonce = new byte[8];
            System.arraycopy(decryptedSt, 0, key, 0, 16);
            System.arraycopy(decryptedSt, 16, nonce, 0, 8);

            return new DecryptionKeys(key, nonce);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to decrypt security token");
            throw new RuntimeException("Failed to decrypt security token", e);
        }
    }
}
