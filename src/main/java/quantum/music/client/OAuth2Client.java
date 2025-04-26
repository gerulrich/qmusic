package quantum.music.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.smallrye.mutiny.Uni;
import quantum.music.model.TokenResponse;

@Path("/v1/oauth2")
@RegisterRestClient(configKey = "auth-api-tdl")
public interface OAuth2Client {

    @POST
    @Path("/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Uni<TokenResponse> renewToken(
            @FormParam("grant_type") String grantType,
            @FormParam("refresh_token") String refreshToken,
            @FormParam("client_id") String clientId,
            @FormParam("scope") String scope
    );
}