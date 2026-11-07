package quantum.music.domain.tdl;

/// Playback information parsed from a TDL media manifest.
///
/// @param urls media segment or file URLs
/// @param encryption encryption scheme reported by the manifest
/// @param keyId identifier for the key used to decrypt the media
public record MediaInfo(
    String []urls,
    String encryption,
    String keyId) {
}
