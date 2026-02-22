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
import quantum.music.api.ApiAlbum;
import quantum.music.api.ApiArtist;
import quantum.music.api.ListResponse;
import quantum.music.api.Response;
import quantum.music.mappers.Mapper;
import quantum.music.service.ProviderService;

/**
 * Resource providing REST endpoints for artist and album information.
 * <p>
 * This resource offers endpoints to retrieve artist details and their associated albums
 * from various music providers. It supports reactive programming using Mutiny's Uni
 * for non-blocking operations and provides paginated results for album listings.
 * </p>
 * <p>
 * All endpoints return data in JSON format and construct URIs dynamically based on
 * the request context to ensure proper resource linking.
 * </p>
 */
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Artists", description = "Artist information and album listings")
@Path("/music")
public class ArtistResource extends Mapper {

    /**
     * Service for interacting with music provider implementations.
     */
    @Inject
    ProviderService providerService;

    /**
     * Retrieves detailed information about a specific artist.
     * <p>
     * This endpoint fetches artist information from the appropriate music provider
     * based on the artist ID. The response includes the artist's basic details
     * and a URI to retrieve their albums.
     * </p>
     *
     * @param uriInfo The URI context information used to construct resource links
     * @param artistId The unique identifier of the artist to retrieve
     * @return A Uni emitting the artist information wrapped in an ApiArtist object
     */
    @GET
    @Path("/artists/{artist}")
    @Authenticated
    public Uni<ApiArtist> artist(
            @Context UriInfo uriInfo,
            @Parameter(description = "Artist unique identifier", required = true, example = "tdl:98765")
            @PathParam("artist") String artistId
    ) {
        return providerService.fromId(artistId)
                .onItem().transformToUni(musicProvider -> musicProvider.getArtistById(artistId))
                .onItem().transform( artist -> detail(getBaseUrl(uriInfo), artist));
    }

    /**
     * Retrieves a paginated list of albums for a specific artist.
     * <p>
     * This endpoint fetches all albums associated with the specified artist from
     * the appropriate music provider. Results are paginated to manage large
     * collections efficiently. Each album includes complete metadata and links
     * to related resources.
     * </p>
     *
     * @param uriInfo The URI context information used to construct resource links
     * @param artistId The unique identifier of the artist whose albums to retrieve
     * @param offset The starting position in the result set (default: 0)
     * @param limit The maximum number of results to return (default: 10)
     * @return A Uni emitting an ItemsResponse containing the paginated album list
     */
    @GET
    @Path("/artists/{artist}/albums")
    @Operation(
        summary = "Get artist albums",
        description = "Retrieves a paginated list of albums for a specific artist with complete metadata"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Albums retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ListResponse.class))
        ),
        @APIResponse(
            responseCode = "404",
            description = "Artist not found"
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    @Authenticated
    public Uni<ListResponse<ApiAlbum>> albums(
            @Context UriInfo uriInfo,
            @Parameter(description = "Artist unique identifier", required = true, example = "tdl:98765")
            @PathParam("artist") String artistId,
            @Parameter(description = "Starting position in the result set", example = "0")
            @QueryParam("offset") @DefaultValue("0") int offset,
            @Parameter(description = "Maximum number of results to return", example = "10")
            @QueryParam("limit") @DefaultValue("10") int limit
    ) {
        return providerService.fromId(artistId)
            .onItem().transformToUni(musicProvider -> musicProvider.getAlbumsByArtistId(artistId, offset, limit))
            .onItem().transform(page -> {
                String baseUrl = getBaseUrl(uriInfo);
                return Response.list(map(page.items(), album -> link(baseUrl, album)))
                    .offset(page.offset())
                    .limit(page.limit())
                    .total(page.total())
                    .build();
            });
    }
}
