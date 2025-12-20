package quantum.music.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import quantum.music.api.AudioSource;

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
public class SourcesResource {

    /**
     * Retrieves a list of available audio sources.
     * <p>
     * Returns all configured audio sources that can be used to play music.
     * Currently includes local and TDL sources.
     * </p>
     *
     * @return a list of {@link AudioSource} objects representing available sources
     */
    @GET
    public List<AudioSource> list() {
        return List.of(
                new AudioSource("local"),
                new AudioSource("tdl")
        );
    }
}
