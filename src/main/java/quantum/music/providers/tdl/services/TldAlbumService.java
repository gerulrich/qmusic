package quantum.music.providers.tdl.services;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import quantum.music.client.ApiClient;
import quantum.music.domain.PagedResponse;
import quantum.music.domain.providers.Album;
import quantum.music.domain.providers.Artist;
import quantum.music.domain.providers.Track;
import quantum.music.domain.providers.TrackList;
import quantum.music.service.TokenService;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class TldAlbumService extends TldAbstractService {

    private static final Logger LOG = Logger.getLogger(TldAlbumService.class);

    @Inject
    @RestClient
    private ApiClient client;

    @Inject
    TokenService tokenService;

    /**
     * Get album details by ID.
     *
     * @param album Album ID
     * @return Album details
     */
    public Uni<Album> getAlbumById(String album) {
        LOG.debugf("Retrieving album details for: %s", album);
        return tokenService.withToken(() -> client.album(parsedId(album))
                .onItem().transform(this::mapAlbum)
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting album: %s", album)));
    }

    public Uni<TrackList> getTracksByAlbumId(String album) {
        LOG.debugf("Retrieving album tracks for: %s", album);
        return tokenService.withToken(() -> client.tracks(parsedId(album))
                .onItem().transform(this::mapAlbumWithTracks)
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting tracks for album: %s", album)));
    }

    public Uni<PagedResponse<Album>> getAlbumsByArtistId(String artistId, int offset, int limit) {
        return tokenService.withToken(() -> {
            LOG.debugf("Retrieving albums for artist: %s", artistId);
            return client.albums(parsedId(artistId), offset, limit)
                    .onItem().transform(json -> mapToPagedResponse(json, offset, limit))
                    .onFailure().invoke(e -> LOG.errorf(e, "Error getting albums for artist: %s", artistId));
        });
    }

    private PagedResponse<Album> mapToPagedResponse(JsonObject json, int offset, int limit) {
        JsonArray items = json.getJsonArray("items");
        List<Album> albums = items.stream()
                .map(JsonObject.class::cast)
                .map(this::mapAlbum)
                .collect(Collectors.toList());
        return new PagedResponse<>(albums, offset, limit, json.getInteger("totalNumberOfItems"));
    }

    private Album mapAlbum(JsonObject json) {
        JsonObject artistJson = json.getJsonObject("artist");
        return Album.builder()
            .id(formatId(json.getLong("id")))
            .title(json.getString("title"))
            .artist(
                Artist.builder()
                    .id(formatId(artistJson.getLong("id")))
                    .name(artistJson.getString("name"))
                    .build()
            )
            .release(json.getString("releaseDate"))
            .copyright(json.getString("copyright"))
            //.type(json.getString("type"))
            .cover(formatCoverUrl(json.getString("cover")))
            .tags(getTags(json))
        .build();
    }

    private TrackList mapAlbumWithTracks(JsonObject json) {
        JsonArray items = json.getJsonArray("items");
        JsonObject albumJson = items.getJsonObject(0).getJsonObject("album");
        JsonObject artistJson = items.getJsonObject(0).getJsonObject("artist");
        return new TrackList(
            Album.builder()
                    .id(formatId(albumJson.getLong("id")))
                    .title(albumJson.getString("title"))
                    .artist(
                        Artist.builder()
                            .id(formatId(artistJson.getLong("id")))
                            .name(artistJson.getString("name"))
                            .build()
                    )
                    .build()
                ,items.stream()
                .map(JsonObject.class::cast)
                .map(this::mapJsonToTrack)
                .collect(Collectors.toList())
        );
    }

    private Track mapJsonToTrack(JsonObject json) {
        return Track.builder()
            .id(formatId(json.getLong("id")))
            .title(json.getString("title"))
            .duration(json.getInteger("duration"))
            .trackNumber(json.getInteger("trackNumber"))
            .volumeNumber(json.getInteger("volumeNumber"))
            // TODO.codec()
            // TODO .quality()
            .build();
    }

    private List<String> getTags(JsonObject json) {
        JsonArray tagsArray = json.getJsonObject("mediaMetadata").getJsonArray("tags");
        if (tagsArray == null || tagsArray.isEmpty()) {
            return null;
        }
        return tagsArray.stream().map(Object::toString).collect(Collectors.toList());
    }


}
