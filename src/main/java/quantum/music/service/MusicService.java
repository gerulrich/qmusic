package quantum.music.service;

import io.vertx.core.json.JsonArray;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import io.vertx.core.json.JsonObject;
import quantum.music.client.ApiClient;
import io.smallrye.mutiny.Uni;
import quantum.music.dto.detail.*;
import quantum.music.dto.summary.*;
import org.jboss.logging.Logger;

import java.util.Base64;
import java.util.List;

import static java.lang.String.format;

/**
 * Service class to handle music-related operations.
 * Provides methods for searching and retrieving information about 
 * artists, albums, and tracks from an external API.
 */
@ApplicationScoped
public class MusicService {
    
    private static final Logger LOG = Logger.getLogger(MusicService.class);
    
    // API search and media constants
    private static final String SEARCH_TYPE_ALBUMS = "ALBUMS";
    private static final String MEDIA_QUALITY_LOSSLESS = "LOSSLESS";
    private static final String MEDIA_TYPE_STREAM = "STREAM";
    private static final String MEDIA_CONTENT_FULL = "FULL";

    @Inject
    @RestClient
    private ApiClient apiClient;

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
        return apiClient.search(q, SEARCH_TYPE_ALBUMS, offset, limit)
                .onItem().transform(json -> {
                    LOG.debugf("Processing search results for query: %s", q);
                    JsonArray items = json.getJsonObject("albums").getJsonArray("items");
                    List<Album> albums = items.stream()
                            .map(JsonObject.class::cast)
                            .map(item -> {
                                Artist artist = createArtistFromJson(
                                        item.getJsonArray("artists").getJsonObject(0));
                                return new Album(
                                        item.getLong("id"),
                                        item.getString("title"),
                                        artist,
                                        formatCoverUrl(item.getString("cover")),
                                        formatResourceUrl("albums", item.getLong("id"))
                                );
                            }).toList();
                    return new ItemsResponse<>(albums, offset, limit, 
                            json.getJsonObject("albums").getInteger("totalNumberOfItems"));
                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error searching for query: %s", q));
    }

    /**
     * Get artist details by ID.
     * 
     * @param artist Artist ID
     * @return Artist details
     */
    public Uni<ArtistDetail> artist(String artist) {
        return apiClient.artist(artist)
                .onItem().transform(json -> {
                    LOG.debugf("Retrieving artist details for: %s", artist);
                    return new ArtistDetail(
                            json.getLong("id"),
                            json.getString("name"),
                            formatResourceUrl("artists", json.getLong("id"), "albums")
                    );
                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting artist: %s", artist));
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
        return apiClient.albums(artist, offset, limit)
                .onItem().transform(json -> {
                    LOG.debugf("Retrieving albums for artist: %s", artist);
                    JsonArray items = json.getJsonArray("items");
                    List<Album> albums = items.stream()
                            .map(JsonObject.class::cast)
                            .map(this::mapJsonToAlbum).toList();
                    return new ItemsResponse<>(albums, offset, limit, json.getInteger("totalNumberOfItems"));
                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting albums for artist: %s", artist));
    }

    /**
     * Get album details by ID.
     * 
     * @param albumId Album ID
     * @return Album details
     */
    public Uni<AlbumDetail> album(String albumId) {
        return apiClient.album(albumId)
                .onItem().transform(json -> {
                    LOG.debugf("Retrieving album details for: %s", albumId);
                    return new AlbumDetail(
                         json.getLong("id"),
                         json.getString("title"),
                         createArtistFromJson(json.getJsonObject("artist")),
                         formatCoverUrl(json.getString("cover")),
                         formatResourceUrl("albums", json.getLong("id"), "tracks"));
                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting album: %s", albumId));
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
                            mapJsonToAlbum(json.getJsonObject("album")),
                            formatResourceUrl("tracks", json.getLong("id"), "content")
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
                            manifest.getString("codecs")
                    );
                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting content for track: %s", track));
    }

    // Helper methods to reduce code duplication
    
    private String formatCoverUrl(String cover) {
        return format(coverUrl, cover.replaceAll("-", "/"));
    }
    
    private String formatResourceUrl(String resourceType, Long id, String... additionalPaths) {
        StringBuilder builder = new StringBuilder(format("%s/%s/%s", domain, resourceType, id));
        for (String path : additionalPaths) {
            builder.append("/").append(path);
        }
        return builder.toString();
    }
    
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

    private Album mapJsonToAlbum(JsonObject item) {
        Artist artist = createArtistFromJson(item.getJsonObject("artist"));
        return new Album(
                item.getLong("id"),
                item.getString("title"),
                artist,
                formatCoverUrl(item.getString("cover")),
                formatResourceUrl("albums", item.getLong("id"))
        );
    }

    private Album mapJsonToAlbum(JsonObject item, JsonObject artistJson) {
        Artist artist = createArtistFromJson(artistJson);
        return new Album(
                item.getLong("id"),
                item.getString("title"),
                artist,
                formatCoverUrl(item.getString("cover")),
                formatResourceUrl("albums", item.getLong("id"))
        );
    }
    
    private Track mapJsonToTrack(JsonObject item) {
        return new Track(
                item.getLong("id"),
                item.getString("title"),
                mapJsonToAlbum(item.getJsonObject("album"), item.getJsonObject("artist")),
                formatResourceUrl("tracks", item.getLong("id"))
        );
    }
}
