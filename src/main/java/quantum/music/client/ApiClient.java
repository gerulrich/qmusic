package quantum.music.client;

import io.quarkus.rest.client.reactive.ClientQueryParam;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.smallrye.mutiny.Uni;

/// Reactive REST client for the remote TDL catalog and playback API.
@Path("/v1")
@RegisterRestClient(configKey = "tdl-api")
@RegisterClientHeaders(ApiHeadersFactory.class)
@ClientQueryParam(name = "countryCode", value = "${tdl.country.code}")

@Produces(MediaType.APPLICATION_JSON)
public interface ApiClient {

    /// Searches the catalog for albums or artists.
    ///
    /// @param query search text
    /// @param types entity types to search
    /// @param offset zero-based result offset
    /// @param limit maximum number of results
    /// @return the remote search response
    @GET
    @Path("/search")
    Uni<JsonObject> search(
        @QueryParam("query") String query,
        @QueryParam("types") String types,
        @QueryParam("offset") int offset,
        @QueryParam("limit") int limit
    );

    /// Retrieves an artist by its provider-specific identifier.
    ///
    /// @param artist artist identifier
    /// @return the artist response
    @GET
    @Path("/artists/{id}")
    Uni<JsonObject> artist(@PathParam("id") String artist);

    /// Retrieves an artist biography.
    ///
    /// @param artist artist identifier
    /// @return the biography response
    @GET
    @Path("/artists/{id}/bio")
    Uni<JsonObject> bio(@PathParam("id") String artist);

    /// Retrieves an artist's albums.
    ///
    /// @param artist artist identifier
    /// @param offset zero-based result offset
    /// @param limit maximum number of results
    /// @return the paginated albums response
    @GET
    @Path("/artists/{id}/albums")
    Uni<JsonObject> albums(
        @PathParam("id") String artist,
        @QueryParam("offset") int offset,
        @QueryParam("limit") int limit
    );

    /// Retrieves an album by its provider-specific identifier.
    ///
    /// @param album album identifier
    /// @return the album response
    @GET
    @Path("/albums/{id}")
    Uni<JsonObject> album(@PathParam("id") String album);

    /// Retrieves an album's tracks.
    ///
    /// @param album album identifier
    /// @return the track-list response
    @GET
    @Path("/albums/{id}/tracks")
    Uni<JsonObject> tracks(@PathParam("id") String album);

    /// Retrieves a track by its provider-specific identifier.
    ///
    /// @param track track identifier
    /// @return the track response
    @GET
    @Path("/tracks/{id}")
    Uni<JsonObject> track(@PathParam("id") String track);

    /// Retrieves playback URLs and stream metadata for a track.
    ///
    /// @param track track identifier
    /// @param audioQuality requested audio quality
    /// @param playbackMode playback mode
    /// @param assetPresentation requested asset presentation
    /// @return the playback information response
    @GET
    @Path("/tracks/{id}/playbackinfopostpaywall")
    Uni<JsonObject> media(
            @PathParam("id") String track,
            @QueryParam("audioquality") String audioQuality,
            @QueryParam("playbackmode") String playbackMode,
            @QueryParam("assetpresentation") String assetPresentation
    );
}