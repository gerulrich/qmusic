package quantum.music.providers.tdl;

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

    public Uni<Album> getTracksByAlbumId(String album) {
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
        return new Album(
            formatId(json.getLong("id")),
            json.getString("title"),
            mapArtist(json.getJsonObject("artist")),
            json.getString("releaseDate"),
            json.getString("copyright"),
            json.getString("type"),
            formatCoverUrl(json.getString("cover")),
            null,
            formatResourceUrl("albums", formatId(json.getLong("id"))));
    }

    /**
     * Maps a JSON object to an Artist.
     *
     * @param json The JSON object representing an artist
     * @return The mapped Artist object, or null if json is null
     */
    private Artist mapArtist(JsonObject json) {
        if (json == null) {
            return null;
        }
        return new Artist(
                formatId(json.getLong("id")),
                json.getString("name"),
                formatResourceUrl("artists", formatId(json.getLong("id")))
        );
    }

    private Album mapAlbumWithTracks(JsonObject json) {
        JsonArray items = json.getJsonArray("items");
        List<Track> tracks = items.stream()
                .map(JsonObject.class::cast)
                .map(this::mapJsonToTrack).toList();
        return new Album(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                tracks,
                null
        );
    }

    private Track mapJsonToTrack(JsonObject json) {
        return new Track(
                formatId(json.getLong("id")),
                json.getString("title"),
                json.getInteger("duration"),
                json.getInteger("trackNumber"),
                json.getInteger("volumeNumber"),
                STR."/tracks/\{formatId(json.getLong("id"))}"
        );
    }

}
