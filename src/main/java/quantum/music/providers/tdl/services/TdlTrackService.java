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
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import quantum.music.client.ApiClient;
import quantum.music.domain.providers.*;
import quantum.music.domain.tdl.MediaInfo;
import quantum.music.service.TokenService;
import quantum.music.providers.tdl.stream.FileStreamer;
import quantum.music.providers.tdl.stream.crypto.DecryptingFileStreamer;
import quantum.music.providers.tdl.stream.http.BasicFileStreamer;
import quantum.music.providers.tdl.stream.http.MultiUrlFileStreamer;
import quantum.music.providers.tdl.manifest.ManifestParser;

import java.util.List;
import java.util.stream.Stream;

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

    @Inject
    ManifestParser manifestParser;

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
                .onItem().ifNull().failWith(() -> new NotFoundException(STR."Track not found: \{trackId}"))
                .onItem().transform(json -> {
                    JsonObject albumNode = json.getJsonObject("album");
                    JsonObject artistNode = json.getJsonObject("artist");
                    List<String> tags = json.getJsonObject("mediaMetadata")
                        .getJsonArray("tags")
                        .stream()
                        .map(Object::toString)
                        .toList();
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
                                .cover(formatImageUrl(albumNode.getString("cover"), COVER_RESOLUTION))
                                .build()
                            ,
                            Track.builder()
                                .id(formatId(json.getLong("id")))
                                .title(json.getString("title"))
                                .duration(json.getInteger("duration"))
                                .trackNumber(json.getInteger("trackNumber"))
                                .volumeNumber(json.getInteger("volumeNumber"))
                                .codec(json.getString("audioCodec"))
                                .quality(json.getString("audioQuality"))
                                .tags(tags)
                                .streams(Stream.concat(
                                    Stream.of(
                                        TrackStream.builder().quality("LOW")
                                            .url(STR."tracks/\{trackId}/stream?quality=LOW")
                                            .build(),
                                        TrackStream.builder().quality("HIGH")
                                            .url(STR."tracks/\{trackId}/stream?quality=HIGH")
                                            .build()
                                    ),
                                    tags.stream().distinct().map(tag -> TrackStream.builder().quality(tag)
                                        .url(STR."tracks/\{trackId}/stream?quality=\{tag}")
                                        .build()
                                        )
                                ).toList())
                                .version(json.getString("version"))
                                .copyright(json.getString("copyright"))
                                .build()
                    );
                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting track: %s", trackId)));
    }

    public Uni<MediaInfo> content(String track, String codec, String quality, String presentation) {
        LOG.debugf("Retrieving media content for track: %s with codec: %s and quality: %s", track, codec, quality);
        String q = quality.replaceAll("HIRES", "HI_RES");
        return tokenService.withToken(() -> apiClient.media(parsedId(track), q, MEDIA_TYPE_STREAM, presentation)
                .onItem().ifNull().failWith(() -> new NotFoundException(STR."Track not found: \{track}"))
                .onItem().transform(json -> {
                    LOG.debugf("Retrieving content for track: %s", parsedId(track));
                    String manifestMimeType = json.getString("manifestMimeType");
                    return manifestParser.parse(manifestMimeType, json.getString("manifest"));
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
        String encryption = mediaInfo.encryption();
        FileStreamer base;
        if (mediaInfo.urls().length > 1) {
            base = new MultiUrlFileStreamer(httpClient, List.of(mediaInfo.urls()));
        } else {
            base = new BasicFileStreamer(httpClient, new RequestOptions()
                .setMethod(HttpMethod.GET)
                .setAbsoluteURI(mediaInfo.urls()[0]));
        }
        FileStreamer streamer = switch (encryption) {
            case NONE -> base;
            case OLD_AES -> new DecryptingFileStreamer(base, mediaInfo.keyId(), masterKey);
            default -> throw new IllegalStateException("Unexpected value: " + encryption);
        };
        return streamer.stream();
    }

}
