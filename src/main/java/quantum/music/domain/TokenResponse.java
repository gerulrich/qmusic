package quantum.music.domain;

public record TokenResponse(
        String access_token,
        String refresh_token,
        long expires_in
) {
}