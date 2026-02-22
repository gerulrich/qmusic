package quantum.music.domain.tdl;

public record MediaInfo(
    String []urls,
    String quality,
    String codec,
    String encryption,
    String keyId) {
}
