package quantum.music.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.smallrye.mutiny.Uni;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import quantum.music.client.OAuth2Client;

import java.time.Instant;
import java.util.function.Supplier;

@ApplicationScoped
public class TokenService {

    @Inject
    @RestClient
    OAuth2Client oAuth2Client;

    @ConfigProperty(name = "tdl.client.id")
    String clientId;

    @ConfigProperty(name = "tdl.refresh.token")
    String refreshToken;

    private String currentToken;
    private Instant expiresAt;

    /**
     * Retrieves the OAuth2 token. If the token is expired or not present, it will renew it.
     *
     * @return A Uni containing the access token.
     */
    public Uni<String> getToken() {
        if (currentToken == null || Instant.now().isAfter(expiresAt)) {
            return renewToken();
        }
        return Uni.createFrom().item(currentToken);
    }

    /**
     * Returns the cached token if it is still valid.
     *
     * @return The cached token or null if it has expired.
     */
    public String getCachedToken() {
        return currentToken;
    }

    /**
     * Executes a function that requires the OAuth2 token. The function will be called with the token.
     *
     * @param fn A function that takes a token and returns a Uni.
     * @param <R> The type of the result.
     * @return A Uni containing the result of the function.
     */
    public <R> Uni<R> withToken(Supplier<Uni<R>> fn) {
        return getToken().onItem().transformToUni(token -> fn.get());
    }

    /**
     * Renews the OAuth2 token using the refresh token.
     *
     * @return A Uni containing the new access token.
     */
    private Uni<String> renewToken() {
        return oAuth2Client.renewToken(
                "refresh_token",
                refreshToken,
                clientId,
                "r_usr+w_usr"
            )
            .onItem().transform(token -> {
                this.currentToken = token.access_token();
                this.expiresAt = Instant.now().plusSeconds(token.expires_in() - 60);
                return currentToken;
            });
    }
}