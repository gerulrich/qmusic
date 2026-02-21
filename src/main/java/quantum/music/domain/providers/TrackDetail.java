package quantum.music.domain.providers;

/**
 * Provider-agnostic view of a single track with its parent album.
 *
 * @param album album metadata for the track
 * @param track track metadata and playback details
 */
public record TrackDetail(Album album, Track track) {
}
