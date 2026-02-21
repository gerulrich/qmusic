package quantum.music.domain.providers;

import java.util.List;

/**
 * Provider-agnostic album with its tracks.
 *
 * @param album album metadata
 * @param tracks ordered track list
 */
public record TrackList(Album album, List<Track> tracks) {
}
