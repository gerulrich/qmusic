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

    public Uni<String> getToken() {
        if (currentToken == null || Instant.now().isAfter(expiresAt)) {
            return renewToken();
        }
        return Uni.createFrom().item(currentToken);
    }

    public String getCachedToken() {
        return currentToken;
    }

    public <R> Uni<R> withToken(Supplier<Uni<R>> fn) {
        return getToken().onItem().transformToUni(token -> fn.get());
    }

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