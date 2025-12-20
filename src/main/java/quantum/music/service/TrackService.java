package quantum.music.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
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
import org.jboss.logging.Logger;
import quantum.music.client.ApiClient;
import quantum.music.dto.detail.DecryptionKeys;
import quantum.music.dto.detail.MediaInfo;
import quantum.music.dto.detail.TrackDetail;
import quantum.music.dto.summary.Album;
import quantum.music.dto.summary.Artist;
import quantum.music.dto.summary.Track;
import quantum.music.dto.summary.TrackList;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

import static java.lang.String.format;

@ApplicationScoped
public class TrackService {

    private static final Logger LOG = Logger.getLogger(TrackService.class);

    private static final String MEDIA_QUALITY_LOSSLESS = "LOSSLESS";
    private static final String MEDIA_TYPE_STREAM = "STREAM";
    private static final String MEDIA_CONTENT_FULL = "FULL";
    public static final String NONE = "NONE";

    @Inject
    @RestClient
    private ApiClient apiClient;

    @ConfigProperty(name = "tdl.cover.url")
    private String coverUrl;

    @ConfigProperty(name = "app.domain")
    private String domain;

    @Inject
    private Vertx vertx;

    private HttpClient httpClient;
    @ConfigProperty(name = "tdl.master.key")
    private String masterKey;


    @PostConstruct
    void init() {
        httpClient = vertx.createHttpClient();
    }

    /**
     * Get tracks for an album.
     *
     * @param album Album ID
     * @return List of tracks
     */
    public Uni<TrackList> tracks(String album) {
        return apiClient.tracks(album)
                .onItem().transform(json -> {
                    LOG.debugf("Retrieving tracks for album: %s", album);
                    JsonArray items = json.getJsonArray("items");
                    List<Track> tracks = items.stream()
                            .map(JsonObject.class::cast)
                            .map(this::mapJsonToTrack).toList();
                    return new TrackList(json.getInteger("totalNumberOfItems"), tracks);
                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting tracks for album: %s", album));
    }

    /**
     * Get track details by ID.
     *
     * @param track Track ID
     * @return Track details
     */
    public Uni<TrackDetail> track(String track) {
        return apiClient.track(track)
                .onItem().transform(json -> {
                    LOG.debugf("Retrieving track details for: %s", track);
                    return new TrackDetail(
                            json.getLong("id"),
                            json.getString("title"),
                            json.getInteger("duration"),
                            json.getInteger("trackNumber"),
                            json.getInteger("volumeNumber"),
                            json.getString("audioQuality"),
                            json.getJsonObject("mediaMetadata").getJsonArray("tags").stream().map(Object::toString).toList(),
                            json.getString("version"),
                            json.getString("copyright"),
                            mapJsonToAlbum(json.getJsonObject("album")),
                            formatResourceUrl("tracks", json.getLong("id"), "stream")
                    );
                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting track: %s", track));
    }

    /**
     * Get media content information for a track.
     *
     * @param track Track ID
     * @return Media information
     */
    public Uni<MediaInfo> content(String track) {
        return apiClient.media(track, MEDIA_QUALITY_LOSSLESS, MEDIA_TYPE_STREAM, MEDIA_CONTENT_FULL)
                .onItem().transform(json -> {
                    LOG.debugf("Retrieving content for track: %s", track);
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
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting content for track: %s", track));
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

    private String formatCoverUrl(String cover) {
        return format(coverUrl, cover.replaceAll("-", "/"));
    }

    /**
     * Formats a resource URL using the configured pattern.
     *
     * @param resourceType The type of resource (e.g., "albums", "tracks")
     * @param id           The ID of the resource
     * @param additionalPaths Additional paths to append to the URL
     * @return The formatted resource URL
     */
    private String formatResourceUrl(String resourceType, Long id, String... additionalPaths) {
        StringBuilder builder = new StringBuilder(format("%s/%s/%s", domain, resourceType, id));
        for (String path : additionalPaths) {
            builder.append("/").append(path);
        }
        return builder.toString();
    }

    /**
     * Maps a JSON object to an Album object.
     *
     * @param item The JSON object representing an album
     * @return The mapped Album object
     */
    private Album mapJsonToAlbum(JsonObject item) {
        Artist artist = createArtistFromJson(item.getJsonObject("artist"));
        return new Album(
                item.getLong("id"),
                item.getString("title"),
                item.getString("release"),
                artist,
                formatCoverUrl(item.getString("cover")),
                formatResourceUrl("albums", item.getLong("id"))
        );
    }

    /**
     * Maps a JSON object to an Album object.
     *
     * @param item The JSON object representing an album
     * @param artistJson The JSON object representing the artist
     * @return The mapped Album object
     */
    private Album mapJsonToAlbum(JsonObject item, JsonObject artistJson) {
        Artist artist = createArtistFromJson(artistJson);
        return new Album(
                item.getLong("id"),
                item.getString("title"),
                item.getString("release"),
                artist,
                formatCoverUrl(item.getString("cover")),
                formatResourceUrl("albums", item.getLong("id"))
        );
    }

    /**
     * Maps a JSON object to an Artist.
     *
     * @param json The JSON object representing an artist
     * @return The mapped Artist object, or null if json is null
     */
    private Artist createArtistFromJson(JsonObject json) {
        if (json == null) {
            return null;
        }
        return new Artist(
                json.getLong("id"),
                json.getString("name"),
                formatResourceUrl("artists", json.getLong("id"))
        );
    }

    /**
     * Maps a JSON object to a Track object.
     *
     * @param item The JSON object representing a track
     * @return The mapped Track object
     */
    private Track mapJsonToTrack(JsonObject item) {
        return new Track(
                item.getLong("id"),
                item.getString("title"),
                item.getInteger("duration"),
                item.getInteger("trackNumber"),
                item.getInteger("volumeNumber"),
                mapJsonToAlbum(item.getJsonObject("album"), item.getJsonObject("artist")),
                formatResourceUrl("tracks", item.getLong("id"))
        );
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
