package quantum.music.domain;

/**
 * Represents an OAuth2 token response in the qmusic domain layer.
 * <p>
 * This record encapsulates the access and refresh tokens, along with the expiration time, as returned
 * by the authentication provider. It is used throughout the application to securely manage user sessions
 * and authorize access to protected musical resources and APIs.
 *
 * @param access_token   the access token string used for authenticating API requests
 * @param refresh_token  the refresh token string used to obtain new access tokens
 * @param expires_in     the lifetime in seconds of the access token before it expires
 */
public record TokenResponse(
        String access_token,
        String refresh_token,
        long expires_in
) {
}