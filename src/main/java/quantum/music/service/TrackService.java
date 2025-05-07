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
import quantum.music.dto.detail.MediaInfo;
import quantum.music.dto.detail.TrackDetail;
import quantum.music.dto.summary.Album;
import quantum.music.dto.summary.Artist;
import quantum.music.dto.summary.Track;
import quantum.music.dto.summary.TrackList;

import java.util.Base64;
import java.util.List;

import static java.lang.String.format;

@ApplicationScoped
public class TrackService {

    private static final Logger LOG = Logger.getLogger(TrackService.class);

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

}
