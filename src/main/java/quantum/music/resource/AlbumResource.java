package quantum.music.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import quantum.music.api.*;

import quantum.music.mappers.Mapper;
import quantum.music.service.ProviderService;

/**
 * Resource providing REST endpoints for album and track information.
 * <p>
 * This resource offers endpoints to retrieve detailed album information and track listings
 * from various music providers. It supports reactive programming using Mutiny's Uni
 * for non-blocking operations and provides comprehensive album metadata including
 * artist details, cover art, copyright information, and complete track listings.
 * </p>
 * <p>
 * All endpoints return data in JSON format and construct URIs dynamically based on
 * the request context to ensure proper resource linking.
 * </p>
 */

@Path("/music")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Albums", description = "Album information and track listings")
public class AlbumResource extends Mapper {

    /**
     * Service for interacting with music provider implementations.
     */
    @Inject
    ProviderService providerService;

    /**
     * Retrieves detailed information about a specific album.
     * <p>
     * This endpoint fetches album information from the appropriate music provider
     * based on the album ID. The response includes the album's complete metadata
     * such as title, release date, cover art, copyright, tags, type, and a URI
     * to retrieve the album's tracks.
     * </p>
     *
     * @param uriInfo The URI context information used to construct resource links
     * @param id      The unique identifier of the album to retrieve
     * @return A Uni emitting the album information wrapped in an ApiAlbum object
     */
    @GET
    @Path("/albums/{id}")
    @Operation(
            summary = "Get album by ID",
            description = "Retrieves detailed information about a specific album including metadata, cover art, and links to tracks"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Album found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiAlbum.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Album not found"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @Authenticated
    public Uni<ApiAlbum> album(
            @Context UriInfo uriInfo,
            @Parameter(description = "Album unique identifier", required = true, example = "tdl:12345")
            @PathParam("id") String id) {
        return providerService.fromId(id)
                .onItem().transformToUni(musicProvider -> musicProvider.getAlbumById(id))
                .onItem().transform(album -> detail(getBaseUrl(uriInfo), album));
    }

    /**
     * Retrieves the track listing for a specific album.
     * <p>
     * This endpoint fetches all tracks associated with the specified album from
     * the appropriate music provider. The response includes complete track information
     * such as title, duration, track number, volume number, and individual track URIs.
     * The result is wrapped in an ApiAlbumTracks object containing the total track count
     * and the list of tracks.
     * </p>
     *
     * @param uriInfo The URI context information used to construct resource links
     * @param id      The unique identifier of the album whose tracks to retrieve
     * @return A Uni emitting an ApiAlbumTracks object containing the track listing
     */
    @GET
    @Path("/albums/{id}/tracks")
    @Operation(
            summary = "Get album tracks",
            description = "Retrieves all tracks for a specific album with complete track information"
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Tracks retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiAlbumTracks.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Album not found"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @Authenticated
    public Uni<ApiAlbumTracks> tracks(
            @Context UriInfo uriInfo,
            @Parameter(description = "Album unique identifier", required = true, example = "tdl:12345")
            @PathParam("id") String id) {
        return providerService.fromId(id)
                .onItem().transformToUni(musicProvider -> musicProvider.getTracksByAlbumId(id))
                .onItem().transform(trackList -> link(getBaseUrl(uriInfo), trackList));
    }
}