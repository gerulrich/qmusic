package quantum.music.mappers;

import jakarta.ws.rs.core.UriInfo;
import quantum.music.api.*;
import quantum.music.domain.providers.*;

import java.util.List;
import java.util.function.Function;

/**
 * Base contract for API mappers that translate domain providers into transport-friendly DTOs.
 * <p>Centralizes repeated logic for building links, composing embedded resources and exposing multiple
 * representations (link, embedded, detail) of artists, albums and tracks.</p>
 * <p>Utility helpers in this class keep URL construction, list mapping and streaming link generation
 * consistent across concrete mapper implementations.</p>
 */
public abstract class Mapper {

    /**
     * Applies the provided mapper to each element and returns an immutable view of the results.
     * @param items source elements that need to be transformed; must not be {@code null}.
     * @param mapper pure function that converts each source element into its DTO counterpart.
     * @return an immutable list containing every mapped element.
     */
    protected <O,T> List<T> map(List<O> items, Function<O, T> mapper) {
        return items.stream().map(mapper).toList();
    }

    /**
     * Derives the canonical API base URL (always rooted under the music context) from the HTTP request.
     * @param uriInfo request-scoped metadata used to obtain the base URI emitted by Quarkus.
     * @return the base URL ending with "/music", used for constructing absolute resource links.
     */
    protected String getBaseUrl(UriInfo uriInfo) {
        return uriInfo.getBaseUri().toString().replaceAll("/$", "/music");
    }

    /**
     * Builds a lightweight artist DTO that exposes only identity and a self link.
     * @param artist domain representation to project to the API layer.
     * @param baseUrl canonical API base URL used for building the self link.
     * @return a DTO pointing to the artist resource.
     */
    protected ApiArtist link(Artist artist, String baseUrl) {
        return ApiArtist.builder()
                .id(artist.id())
                .name(artist.name())
                .link(STR."\{baseUrl}/artists/\{artist.id()}")
                .build();
    }

    /**
     * Builds a detailed artist DTO, including biography and the link to the albums collection.
     * @param baseUrl canonical API base URL used for building nested links.
     * @param artist domain representation to project to the API layer.
     * @return a DTO exposing extra artist metadata and related collections.
     */
    protected ApiArtist detail(String baseUrl, Artist artist) {
        return ApiArtist.builder()
                .id(artist.id())
                .name(artist.name())
                .bio(artist.bio())
                .picture(artist.picture())
                .albums(STR."\{baseUrl}/artists/\{artist.id()}/albums")
                .build();
    }

    /**
     * Creates a lightweight album DTO for listing contexts, exposing the base metadata, tags, type and link.
     * @param baseUrl canonical API base URL used for composing nested links.
     * @param album domain album that will be exposed via the API.
     * @return a DTO ready to be returned from album listing endpoints.
     */
    protected ApiAlbum link(String baseUrl, Album album) {
        return ApiAlbum.builder()
            .id(album.id())
            .title(album.title())
            .volumes(album.volumes())
            .release(album.release())
            .artist(link(album.artist(), baseUrl))
            .cover(album.cover())
            .tags(album.tags())
            .type(album.type())
            .link(STR."\{baseUrl}/albums/\{album.id()}")
            .build();
    }

    /**
     * Generates an embedded album DTO suitable for nesting inside track or artist responses.
     * @param baseUrl canonical API base URL used for the self link.
     * @param album domain album to embed.
     * @return a compact DTO containing only identity, title, artist and link.
     */
    protected ApiAlbum embedded(String baseUrl, Album album) {
        return ApiAlbum.builder()
                .id(album.id())
                .title(album.title())
                .artist(link(album.artist(), baseUrl))
                .cover(album.cover())
                .link(STR."\{baseUrl}/albums/\{album.id()}")
                .build();
    }

    /**
     * Produces a detailed album DTO enriched with copyright, tags and a link to the tracks endpoint.
     * @param baseUrl canonical API base URL used for composing nested links.
     * @param album domain album that will be exposed via the API.
     * @return a DTO ready for album detail endpoints.
     */
    protected ApiAlbum detail(String baseUrl, Album album) {
        return ApiAlbum.builder()
            .id(album.id())
            .title(album.title())
            .release(album.release())
            .artist(link(album.artist(), baseUrl))
            .cover(album.cover())
            .copyright(album.copyright())
            .tags(album.tags())
            .tracks(STR."\{baseUrl}/albums/\{album.id()}/tracks")
            .build();
    }

    /**
     * Builds the DTO representation for the tracks collection inside an album.
     * @param baseUrl canonical API base URL used for composing track links.
     * @param trackList domain aggregate containing the album and its tracks.
     * @return a DTO with the total count and the list of linked tracks.
     */
    protected ApiAlbumTracks link(String baseUrl, TrackList trackList) {
        return ApiAlbumTracks.builder()
                .total(trackList.tracks().size())
                .tracks(map(trackList.tracks(), track -> link(baseUrl, trackList.album(), track)))
                .build();
    }

    /**
     * Creates a lightweight track DTO suitable for listings, pointing back to its album.
     * @param baseUrl canonical API base URL used for the self link.
     * @param album album that owns the track.
     * @param track domain track to expose.
     * @return a DTO with identity, numbering, duration and links.
     */
    protected ApiTrack link(String baseUrl, Album album, Track track) {
        return ApiTrack.builder()
                .id(track.id())
                .title(track.title())
                .duration(track.duration())
                .track(track.trackNumber())
                .volume(track.volumeNumber())
                .album(embedded(baseUrl, album))
                .link(STR."\{baseUrl}/tracks/\{track.id()}")
                .build();
    }

    /**
     * Generates a detailed track DTO, including codec, tags and derived streaming URLs.
     * @param baseUrl canonical API base URL used for nested links.
     * @param trackDetail aggregate that bundles the track with its album metadata.
     * @return a DTO fit for the track detail endpoint, including generated stream links.
     */
    protected ApiTrack detail(String baseUrl, TrackDetail trackDetail) {
        Track track = trackDetail.track();
        Album album = trackDetail.album();
        return ApiTrack.builder()
                .id(track.id())
                .title(track.title())
                .duration(track.duration())
                .track(track.trackNumber())
                .volume(track.volumeNumber())
                .album(embedded(baseUrl, album))
                .codec(track.codec())
                .tags(track.tags())
                .version(track.version())
                .copyright(track.copyright())
                .streams(map(track.streams(), stream -> link(baseUrl, stream)))
                .build();
    }

    /**
     * Creates a streaming descriptor for a specific track and quality, encoding the playback URL.
     * @param baseUrl canonical API base URL used for the streaming endpoint.
     * @param stream domain stream containing the quality and the URL for playback.
     * @return a DTO linking to the stream endpoint with codec and quality information.
     */
    protected ApiTrackStream link(String baseUrl, TrackStream stream) {
        return ApiTrackStream.builder()
                .quality(stream.quality())
                .url(STR."\{baseUrl}/\{stream.url()}")
                .build();
    }
}
