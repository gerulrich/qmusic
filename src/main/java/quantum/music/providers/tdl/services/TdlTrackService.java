package quantum.music.providers.tdl.services;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import quantum.music.client.ApiClient;
import quantum.music.domain.providers.Album;
import quantum.music.domain.providers.Artist;
import quantum.music.domain.providers.Track;
import quantum.music.domain.tdl.MediaInfo;
import quantum.music.domain.providers.TrackDetail;
import quantum.music.service.TokenService;
import quantum.music.providers.tdl.stream.FileStreamer;
import quantum.music.providers.tdl.stream.crypto.DecryptingFileStreamer;
import quantum.music.providers.tdl.stream.http.BasicFileStreamer;

import java.util.Base64;

import static io.quarkus.arc.ComponentsProvider.LOG;

@ApplicationScoped
public class TdlTrackService extends TldAbstractService  {

    private static final String MEDIA_TYPE_STREAM = "STREAM";
    public static final String NONE = "NONE";
    public static final String OLD_AES = "OLD_AES";

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

    public Uni<TrackDetail> getTrackById(String trackId) {
        LOG.debugf("Retrieving track details for: %s", trackId);
        return tokenService.withToken(() -> apiClient.track(parsedId(trackId))
                .onItem().transform(json -> {
                    JsonObject albumNode = json.getJsonObject("album");
                    JsonObject artistNode = json.getJsonObject("artist");

                    return new TrackDetail(
                            Album.builder()
                                .id(formatId(albumNode.getLong("id")))
                                .title(albumNode.getString("title"))
                                .artist(
                                    Artist.builder()
                                            .id(formatId(artistNode.getLong("id")))
                                            .name(artistNode.getString("name"))
                                        .build()
                                )
                                .cover(formatCoverUrl(albumNode.getString("cover")))
                                .build()
                            ,
                            new Track(
                        formatId(json.getLong("id")),
                        json.getString("title"),
                        json.getInteger("duration"),
                        json.getInteger("trackNumber"),
                        json.getInteger("volumeNumber"),
                        json.getString("audioQuality"),
                        json.getString("audioCodec"),
                        json.getJsonObject("mediaMetadata").getJsonArray("tags").stream().map(Object::toString).toList(),
                        json.getString("version"),
                        json.getString("copyright"),
                        formatResourceUrl("tracks", formatId(json.getLong("id")), "stream")
                        )
                    );
                })
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
        FileStreamer base = new BasicFileStreamer(httpClient, new RequestOptions().setMethod(HttpMethod.GET).setAbsoluteURI(url));
        FileStreamer streamer = switch (encryption) {
            case NONE -> base;
            case OLD_AES -> new DecryptingFileStreamer(base, mediaInfo.keyId(), masterKey);
            default -> throw new IllegalStateException("Unexpected value: " + encryption);
        };
        return streamer.stream();
    }


}
