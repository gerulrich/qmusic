package quantum.music.service;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import quantum.music.client.ApiClient;
import quantum.music.dto.detail.AlbumDetail;
import quantum.music.dto.summary.Album;
import quantum.music.dto.summary.Artist;
import quantum.music.dto.summary.ItemsResponse;

import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

@ApplicationScoped
public class AlbumService {

    private static final Logger LOG = Logger.getLogger(AlbumService.class);

    private static final String SEARCH_TYPE_ALBUMS = "ALBUMS";

    @Inject
    @RestClient
    private ApiClient client;

    @ConfigProperty(name = "tdl.cover.url")
    private String coverUrl;

    @ConfigProperty(name = "app.domain")
    private String domain;

    /**
     * Search for albums matching the provided query.
     *
     * @param q Search query
     * @param offset Pagination offset
     * @param limit Page size limit
     * @return Response containing album items
     */
    public Uni<ItemsResponse<Album>> search(String q, int offset, int limit) {
        return client.search(q, SEARCH_TYPE_ALBUMS, offset, limit)
                .onItem().transform(json -> {
                    LOG.debugf("Processing search results for query: %s", q);
                    return jsonToAlbumList(json, offset, limit);
                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error searching for query: %s", q));
    }

    /**
     * Get albums by artist ID.
     *
     * @param artist Artist ID
     * @param offset Pagination offset
     * @param limit Page size limit
     * @return Response containing album items
     */
    public Uni<ItemsResponse<Album>> albums(String artist, int offset, int limit) {
        return client.albums(artist, offset, limit)
                .onItem().transform(json -> {
                    LOG.debugf("Retrieving albums for artist: %s", artist);
                    return mapArtistAlbums(json, offset, limit);
                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting albums for artist: %s", artist));
    }

    /**
     * Get album details by ID.
     *
     * @param album Album ID
     * @return Album details
     */
    public Uni<AlbumDetail> album(String album) {
        return client.album(album)
                .onItem().transform(json -> {
                    LOG.debugf("Retrieving album details for: %s", album);
                    return mapAlbumDetail(json);
                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting album: %s", album));
    }

    public ItemsResponse<Album> jsonToAlbumList(JsonObject json, int offset, int limit) {
        JsonArray items = json.getJsonObject("albums").getJsonArray("items");
        List<Album> albums = items.stream()
                .map(JsonObject.class::cast)
                .map(item -> {
                    Artist artist = mapArtistFromJson(
                            item.getJsonArray("artists").getJsonObject(0));
                    return new Album(
                            item.getLong("id"),
                            item.getString("title"),
                            item.getString("releaseDate"),
                            artist,
                            formatCoverUrl(item.getString("cover")),
                            formatResourceUrl("albums", item.getLong("id"))
                    );
                }).collect(Collectors.toList());

        return new ItemsResponse<>(albums, offset, limit,
                json.getJsonObject("albums").getInteger("totalNumberOfItems"));
    }

    /**
     * Maps a JSON object to an Artist.
     *
     * @param json The JSON object representing an artist
     * @return The mapped Artist object, or null if json is null
     */
    public Artist mapArtistFromJson(JsonObject json) {
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
     * Formats a cover URL using the configured pattern.
     *
     * @param cover The cover identifier
     * @return The formatted cover URL
     */
    private String formatCoverUrl(String cover) {
        return format(coverUrl, cover.replaceAll("-", "/"));
    }

    /**
     * Formats a resource URL using the configured domain.
     *
     * @param resourceType The type of resource (albums, artists, etc.)
     * @param id The resource ID
     * @param additionalPaths Optional additional path segments
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
     * Maps album detail from JSON to an AlbumDetail object.
     *
     * @param json The JSON response from the API
     * @return The mapped AlbumDetail object
     */
    public AlbumDetail mapAlbumDetail(JsonObject json) {
        return new AlbumDetail(
                json.getLong("id"),
                json.getString("title"),
                mapArtistFromJson(json.getJsonObject("artist")),
                json.getString("releaseDate"),
                json.getString("copyright"),
                json.getString("type"),
                formatCoverUrl(json.getString("cover")),
                formatResourceUrl("albums", json.getLong("id"), "tracks"));
    }

    /**
     * Maps artist albums from JSON to an ItemsResponse containing Albums.
     *
     * @param json The JSON response from the API
     * @param offset Pagination offset
     * @param limit Page size limit
     * @return Response with mapped Album objects
     */
    public ItemsResponse<Album> mapArtistAlbums(JsonObject json, int offset, int limit) {
        JsonArray items = json.getJsonArray("items");
        List<Album> albums = items.stream()
                .map(JsonObject.class::cast)
                .map(this::mapAlbumFromJson)
                .collect(Collectors.toList());

        return new ItemsResponse<>(albums, offset, limit, json.getInteger("totalNumberOfItems"));
    }

    /**
     * Maps a JSON object to an Album.
     *
     * @param item The JSON object representing an album
     * @return The mapped Album object
     */
    public Album mapAlbumFromJson(JsonObject item) {
        Artist artist = mapArtistFromJson(item.getJsonObject("artist"));
        return new Album(
                item.getLong("id"),
                item.getString("title"),
                item.getString("releaseDate"),
                artist,
                formatCoverUrl(item.getString("cover")),
                formatResourceUrl("albums", item.getLong("id"))
        );
    }
}