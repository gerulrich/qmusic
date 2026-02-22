package quantum.music.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
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
import org.jboss.resteasy.reactive.RestStreamElementType;
import quantum.music.api.ApiTrack;
import quantum.music.mappers.Mapper;
import quantum.music.service.ProviderService;


@Path("/music")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Tracks", description = "Track information and audio streaming")
public class TrackResource extends Mapper {

    @Inject
    ProviderService providerService;

    /**
     * Retrieves detailed information about a specific track.
     * <p>
     * This endpoint fetches track information from the appropriate music provider
     * based on the track ID. The response includes complete track metadata such as
     * title, duration, codec, quality options, and streaming URIs.
     * </p>
     *
     * @param uriInfo The URI context information used to construct resource links
     * @param id The unique identifier of the track to retrieve
     * @return A Uni emitting the track information wrapped in an ApiTrack object
     */
    @GET
    @Path("/tracks/{id}")
    @Operation(
        summary = "Get track by ID",
        description = "Retrieves detailed information about a specific track including metadata and streaming options"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Track found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ApiTrack.class))
        ),
        @APIResponse(
            responseCode = "404",
            description = "Track not found"
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    @Authenticated
    public Uni<ApiTrack> track(
            @Context UriInfo uriInfo,
            @Parameter(description = "Track unique identifier", required = true, example = "tdl:54321")
            @PathParam("id") String id) {
        return providerService.fromId(id)
                .onItem().transformToUni(musicProvider -> musicProvider.getTrackById(id))
                .onItem().transform( track -> detail(getBaseUrl(uriInfo), track));
    }

    /**
     * Streams audio content for a specific track.
     * <p>
     * This endpoint provides direct audio streaming from the appropriate music provider.
     * It supports multiple codecs and quality levels specified via query parameters.
     * The audio data is streamed reactively using Mutiny's Multi for efficient
     * transmission and low memory footprint.
     * </p>
     *
     * @param id The unique identifier of the track to stream
     * @param codec The audio codec to use for streaming (e.g., "flac", "mp3")
     * @param quality The quality level for the stream (e.g., "HIGH", "LOW")
     * @param presentation The presentation mode (default: "FULL")
     * @return A Multi streaming audio buffer chunks
     */
    @GET
    @Path("/tracks/{id}/stream")
    @Produces("audio/flac")
    @RestStreamElementType(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(
        summary = "Stream track audio",
        description = "Streams audio content for a specific track with support for different codecs and quality levels"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Audio stream started successfully",
            content = @Content(mediaType = "audio/flac")
        ),
        @APIResponse(
            responseCode = "404",
            description = "Track not found"
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public Multi<Buffer> stream(
            @Parameter(description = "Track unique identifier", required = true, example = "tdl:54321")
            @PathParam("id") String id,
            @Parameter(description = "Audio codec", example = "flac")
            @QueryParam("codec") String codec,
            @Parameter(description = "Audio quality level", example = "HIGH")
            @QueryParam("quality") String quality,
            @Parameter(description = "Presentation mode", example = "FULL")
            @QueryParam("presentation") @DefaultValue("FULL") String presentation) {
        return providerService.fromId(id)
            .onItem().transformToMulti(musicProvider -> musicProvider.streamTrackById(id, codec, quality, presentation));
    }
}
