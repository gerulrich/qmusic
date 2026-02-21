package quantum.music.domain.tdl;

public record MediaInfo(
    String url,
    String quality,
    String codec,
    String encryption,
    String keyId) {

    }
