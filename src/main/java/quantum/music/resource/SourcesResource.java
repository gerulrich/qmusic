package quantum.music.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import quantum.music.api.AudioSource;
import quantum.music.service.ProviderService;

import java.util.List;

/**
 * REST resource for managing audio sources.
 * <p>
 * This resource provides endpoints to retrieve available audio sources
 * for the music application.
 * </p>
 */
@Path("/music/sources")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Sources", description = "Audio source management")
public class SourcesResource {

    @Inject
    ProviderService providerService;

    /**
     * Retrieves a list of available audio sources.
     * <p>
     * Returns all configured sources that provide the music catalog and streams,
     * for example local storage and TDL.
     * </p>
     *
     * @return a list of {@link AudioSource} objects representing available sources
     */
    @GET
    @Operation(
        summary = "List audio sources",
        description = "Retrieves all configured sources that provide the music catalog and streams"
    )
    @APIResponses({
        @APIResponse(
            responseCode = "200",
            description = "Sources retrieved successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = AudioSource.class))
        ),
        @APIResponse(
            responseCode = "500",
            description = "Internal server error"
        )
    })
    public List<AudioSource> list() {
        return providerService.getProviders().stream().map(provider ->
                new AudioSource(provider.getItem1(), provider.getItem2(), provider.getItem3())
        ).toList();
    }
}
