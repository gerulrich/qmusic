package quantum.music.resource;

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
import quantum.music.api.ListResponse;
import quantum.music.api.Response;
import quantum.music.mappers.Mapper;
import quantum.music.service.ProviderService;

@Path("/music")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Search", description = "Search for albums across music sources")
public class SearchResource extends Mapper {

    /**
     * Service for interacting with music provider implementations.
     */
    @Inject
    ProviderService providerService;

    /**
     * Searches for albums matching the specified query across music sources.
     * <p>
     * This endpoint performs a search operation on the selected source using
     * the provided query string. Results are paginated to manage large result sets
     * efficiently. Each album in the response includes complete metadata, artist
     * information, and dynamically constructed URIs for accessing related resources.
     * </p>
     * <p>
     * The search is performed reactively using Mutiny's Uni to ensure non-blocking
     * operation and optimal resource utilization.
     * </p>
     *
     * @param uriInfo  The URI context information used to construct resource links
     * @param provider The source that provides the music catalog and streams (default: "tdl")
     * @param query    The search query string to match against album and artist names
     * @param offset   The starting position in the result set (default: 0)
     * @param limit    The maximum number of results to return (default: 10)
     * @return A Uni emitting an ItemsResponse containing the paginated search results
     */
    @GET
    @Path("/search")
    @Operation(
        summary = "Search albums",
        description = "Searches for albums matching the query across music sources with paginated results"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ListResponse.class))
        ),
        @APIResponse(
            responseCode = "400",
            description = "Invalid search parameters"
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public Uni<ListResponse<ApiAlbum>> search(
            @Context UriInfo uriInfo,
            @Parameter(description = "Source that provides the music catalog and streams", example = "tdl")
            @QueryParam("source") @DefaultValue("tdl") String provider,
            @Parameter(description = "Search query string", required = true, example = "The Beatles")
            @QueryParam("q") String query,
            @Parameter(description = "Starting position in the result set", example = "0")
            @QueryParam("offset") @DefaultValue("0") int offset,
            @Parameter(description = "Maximum number of results to return", example = "10")
            @QueryParam("limit") @DefaultValue("10") int limit
    ) {
        return providerService.getProvider(provider)
            .onItem().transformToUni(musicProvider -> musicProvider.search(query, offset, limit))
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
