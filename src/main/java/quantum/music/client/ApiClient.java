package quantum.music.client;

import io.quarkus.rest.client.reactive.ClientQueryParam;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.smallrye.mutiny.Uni;

@Path("/v1")
@RegisterRestClient(configKey = "tdl-api")
@RegisterClientHeaders(ApiHeadersFactory.class)
@ClientQueryParam(name = "countryCode", value = "${tdl.country.code}")

@Produces(MediaType.APPLICATION_JSON)
public interface ApiClient {

    @GET
    @Path("/search")
    Uni<JsonObject> search(
        @QueryParam("query") String query,
        @QueryParam("types") String types,
        @QueryParam("offset") int offset,
        @QueryParam("limit") int limit
    );

    @GET
    @Path("/artists/{id}")
    Uni<JsonObject> artist(@PathParam("id") String artist);

    @GET
    @Path("/artists/{id}/bio")
    Uni<JsonObject> bio(@PathParam("id") String artist);

    @GET
    @Path("/artists/{id}/albums")
    Uni<JsonObject> albums(
        @PathParam("id") String artist,
        @QueryParam("offset") int offset,
        @QueryParam("limit") int limit
    );

    @GET
    @Path("/albums/{id}")
    Uni<JsonObject> album(@PathParam("id") String album);

    @GET
    @Path("/albums/{id}/tracks")
    Uni<JsonObject> tracks(@PathParam("id") String album);

    @GET
    @Path("/tracks/{id}")
    Uni<JsonObject> track(@PathParam("id") String track);

    @GET
    @Path("/tracks/{id}/playbackinfopostpaywall")
    Uni<JsonObject> media(
            @PathParam("id") String track,
            @QueryParam("audioquality") String audioQuality,
            @QueryParam("playbackmode") String playbackMode,
            @QueryParam("assetpresentation") String assetPresentation
    );
}