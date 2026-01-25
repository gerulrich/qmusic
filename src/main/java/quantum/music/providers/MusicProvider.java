package quantum.music.providers;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import quantum.music.domain.providers.*;
import quantum.music.domain.PagedResponse;

import java.util.List;

/**
 * MusicProvider defines the contract for music data providers.
 * <p>
 * Implementations of this interface are responsible for providing access to music-related data such as artists, albums, tracks,
 * and streaming capabilities. The interface supports asynchronous and reactive operations using Mutiny's Uni and Multi types.
 * </p>
 * <p>
 * Typical implementations may wrap third-party music APIs or local music databases.
 * </p>
 */
public interface MusicProvider {

    /**
     * Returns the unique identifier for this provider.
     *
     * @return the provider ID
     */
    String getProviderId();

    /**
     * Returns the human-readable name of this provider.
     *
     * @return the provider name
     */
    String getProviderName();

    /**
     * Returns a list of capabilities supported by this provider (e.g., "list", "play").
     *
     * @return list of capability strings
     */
    List<String> getCapabilities();

    /**
     * Searches for albums matching the given query string.
     *
     * @param q      the search query
     * @param offset the result offset for pagination
     * @param limit  the maximum number of results to return
     * @return a Uni emitting a paged response of albums
     */
    Uni<PagedResponse<Album>> search(String q, int offset, int limit);

    /**
     * Retrieves an artist by their unique identifier.
     *
     * @param artistId the artist's unique ID
     * @return a Uni emitting the artist, or null if not found
     */
    Uni<Artist> getArtistById(String artistId);

    /**
     * Retrieves albums for a given artist.
     *
     * @param artistId the artist's unique ID
     * @param offset   the result offset for pagination
     * @param limit    the maximum number of results to return
     * @return a Uni emitting a paged response of albums
     */
    Uni<PagedResponse<Album>> getAlbumsByArtistId(String artistId, int offset, int limit);

    /**
     * Retrieves an album by its unique identifier.
     *
     * @param albumId the album's unique ID
     * @return a Uni emitting the album, or null if not found
     */
    Uni<Album> getAlbumById(String albumId);

    /**
     * Retrieves the list of tracks for a given album.
     *
     * @param albumId the album's unique ID
     * @return a Uni emitting the track list
     */
    Uni<TrackList> getTracksByAlbumId(String albumId);

    /**
     * Retrieves detailed information for a specific track.
     *
     * @param trackId the track's unique ID
     * @return a Uni emitting the track details
     */
    Uni<TrackDetail> getTrackById(String trackId);

    /**
     * Streams the audio data for a specific track.
     *
     * @param trackId      the track's unique ID
     * @param codec        the desired audio codec (e.g., "mp3", "flac")
     * @param quality      the desired audio quality (e.g., "high", "medium", "low")
     * @param presentation the presentation type (e.g., "full", "preview")
     * @return a Multi emitting Buffer chunks of the audio stream
     */
    Multi<Buffer> streamTrackById(String trackId, String codec, String quality, String presentation);
}
