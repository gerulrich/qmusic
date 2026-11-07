package quantum.music.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import quantum.music.client.OAuth2Client;

import java.time.Instant;
import java.util.function.Supplier;

/// Obtains and caches OAuth2 access tokens for TDL API requests.
@ApplicationScoped
public class TokenService {

    public static final String REFRESH_TOKEN_GRANT = "refresh_token";
    public static final String SCOPE = "r_usr+w_usr";
    @Inject
    @RestClient
    OAuth2Client oAuth2Client;

    @ConfigProperty(name = "tdl.client.id")
    String clientId;

    @ConfigProperty(name = "tdl.refresh.token")
    String refreshToken;

    private String currentToken;
    private Instant expiresAt;

    /// Retrieves the OAuth2 token. If the token is expired or not present, it will renew it.
    ///
    /// @return A Uni containing the access token.
    public Uni<String> getToken() {
        if (currentToken == null || Instant.now().isAfter(expiresAt)) {
            return renewToken();
        }
        return Uni.createFrom().item(currentToken);
    }

    /// Returns the cached token, previously validated by `getToken()`. If the token is expired,
    /// it will return the expired token.
    ///
    /// @return The cached token.
    public String getCachedToken() {
        return currentToken;
    }

    /// Executes a function that requires the OAuth2 token. The function will be called with the token.
    ///
    /// @param fn A function that takes a token and returns a Uni.
    /// @param <R> The type of the result.
    /// @return A Uni containing the result of the function.
    public <R> Uni<R> withToken(Supplier<Uni<R>> fn) {
        return getToken().onItem().transformToUni(token -> fn.get());
    }

    /// Renews the OAuth2 token using the refresh token.
    ///
    /// @return A Uni containing the new access token.
    private Uni<String> renewToken() {
        return oAuth2Client.renewToken(REFRESH_TOKEN_GRANT, refreshToken, clientId, SCOPE)
            .onItem().transform(token -> {
                this.currentToken = token.access_token();
                this.expiresAt = Instant.now().plusSeconds(token.expires_in() - 60);
                return currentToken;
            });
    }
}