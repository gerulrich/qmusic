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
import quantum.music.providers.tdl.TdlMusicProvider;
import quantum.music.service.TokenService;

import java.util.List;
import java.util.stream.Collectors;

/// Searches the TDL catalog and maps paginated album and artist results.
@ApplicationScoped
public class TldSearchService extends TldAbstractService {

    private static final Logger LOG = Logger.getLogger(TdlMusicProvider.class);

    private static final String SEARCH_TYPE_ALBUMS = "ALBUMS";
    private static final String SEARCH_TYPE_ARTISTS = "ARTISTS";

    @Inject
    @RestClient
    private ApiClient client;

    @Inject
    TokenService tokenService;

    /// Searches TDL for albums matching the query.
    ///
    /// @param q search text
    /// @param offset zero-based result offset
    /// @param limit maximum number of results
    /// @return the paginated album results
    public Uni<PagedResponse<Album>> search(String q, int offset, int limit) {
        if (q == null || q.isBlank()) {
            LOG.debugf("TDL search skipped: blank query (offset=%d, limit=%d)", offset, limit);
            return Uni.createFrom().item(new PagedResponse<>(List.of(), offset, limit, 0));
        }
        return tokenService.withToken(() -> client.search(q, SEARCH_TYPE_ALBUMS, offset, limit)
            .onItem().transform(this::mapAlbumPage)
            .onFailure().invoke(e -> LOG.errorf(e, "Error searching for query: %s", q)));
    }

    /// Searches TDL for artists matching the query.
    ///
    /// @param q search text
    /// @param offset zero-based result offset
    /// @param limit maximum number of results
    /// @return the paginated artist results
    public Uni<PagedResponse<Artist>> searchArtists(String q, int offset, int limit) {
        if (q == null || q.isBlank()) {
            LOG.debugf("TDL artist search skipped: blank query (offset=%d, limit=%d)", offset, limit);
            return Uni.createFrom().item(new PagedResponse<>(List.of(), offset, limit, 0));
        }
        return tokenService.withToken(() -> client.search(q, SEARCH_TYPE_ARTISTS, offset, limit)
            .onItem().transform(this::mapArtistPage)
            .onFailure().invoke(e -> LOG.errorf(e, "Error searching artists for query: %s", q)));
    }

    private PagedResponse<Album> mapAlbumPage(JsonObject json) {
        JsonObject albumsJson = json.getJsonObject("albums");
        int offset = albumsJson.getInteger("offset");
        int limit = albumsJson.getInteger("limit");
        int total = albumsJson.getInteger("totalNumberOfItems");
        List<Album> albums = albumsJson.getJsonArray("items").stream()
                .map(JsonObject.class::cast)
                .map(this::mapAlbum)
                .collect(Collectors.toList());
        return new PagedResponse<>(albums, offset, limit, total);
    }

    private PagedResponse<Artist> mapArtistPage(JsonObject json) {
        JsonObject artistsJson = json.getJsonObject("artists");
        if (artistsJson == null) {
            return new PagedResponse<>(List.of(), 0, 0, 0);
        }
        int offset = artistsJson.getInteger("offset", 0);
        int limit = artistsJson.getInteger("limit", 0);
        int total = artistsJson.getInteger("totalNumberOfItems", 0);
        List<Artist> artists = artistsJson.getJsonArray("items", new JsonArray()).stream()
            .map(JsonObject.class::cast)
            .map(this::mapArtist)
            .collect(Collectors.toList());
        return new PagedResponse<>(artists, offset, limit, total);
    }

    private Album mapAlbum(JsonObject json) {
        JsonObject artistNode = getArtistNode(json);
        return Album.builder()
            .id(formatId(json.getLong("id")))
            .title(json.getString("title"))
            .volumes(json.getInteger("numberOfVolumes"))
            .artist(
                Artist.builder()
                    .id(formatId(artistNode.getLong("id")))
                    .name(artistNode.getString("name"))
                    .build()
            )
            .release(json.getString("releaseDate"))
            .copyright(json.getString("copyright"))
            .type(json.getString("type"))
            .cover(formatImageUrl(json.getString("cover"), COVER_RESOLUTION))
            .tags(getTags(json))
            .build();
    }

    private Artist mapArtist(JsonObject json) {
        return Artist.builder()
            .id(formatId(json.getLong("id")))
            .name(json.getString("name"))
            .picture(formatImageUrl(json.getString("picture"), ARTIST_RESOLUTION))
            .build();
    }

    private List<String> getTags(JsonObject json) {
        JsonArray tagsArray = json.getJsonObject("mediaMetadata").getJsonArray("tags");
        if (tagsArray == null || tagsArray.isEmpty()) {
            return null;
        }
        return tagsArray.stream().map(Object::toString).collect(Collectors.toList());
    }

    private JsonObject getArtistNode(JsonObject json) {
        return json.getJsonArray("artists").getJsonObject(0);
    }
}
