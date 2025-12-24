package quantum.music.domain.providers;

public record MediaInfo(
    String url,
    String quality,
    String codec,
    String encryption,
    String keyId) {

    }
