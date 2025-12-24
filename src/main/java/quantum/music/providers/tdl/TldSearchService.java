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
import quantum.music.service.TokenService;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class TldSearchService extends TldAbstractService {

    private static final Logger LOG = Logger.getLogger(TdlMusicProvider.class);

    private static final String SEARCH_TYPE_ALBUMS = "ALBUMS";

    @Inject
    @RestClient
    private ApiClient client;

    @Inject
    TokenService tokenService;

    public Uni<PagedResponse<Album>> search(String q, int offset, int limit) {
        return tokenService.withToken(() -> client.search(q, SEARCH_TYPE_ALBUMS, offset, limit)
                .onItem().transform(json -> {
                    LOG.debugf("Processing search results for query: %s", q);
                    return mapAlbumPage(json, offset, limit);
                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error searching for query: %s", q)));
    }

    private PagedResponse<Album> mapAlbumPage(JsonObject json, int offset, int limit) {
        JsonArray items = json.getJsonObject("albums").getJsonArray("items");
        List<Album> albums = items.stream()
                .map(JsonObject.class::cast)
                .map(this::mapAlbum)
                .collect(Collectors.toList());
        return new PagedResponse<>(albums, offset, limit, getTotalItems(json));
    }

    private Album mapAlbum(JsonObject json) {
        return new Album(
            formatId(json.getLong("id")),
            json.getString("title"),
            mapArtist(getArtistNode(json)),
            json.getString("releaseDate"),
            json.getString("copyright"),
            json.getString("type"),
            formatCoverUrl(json.getString("cover")),
            null,
            formatResourceUrl("albums", formatId(json.getLong("id")))
        );
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

    private JsonObject getArtistNode(JsonObject json) {
        return json.getJsonArray("artists").getJsonObject(0);
    }

    private Integer getTotalItems(JsonObject json) {
        return json.getJsonObject("albums").getInteger("totalNumberOfItems");
    }
}
