package quantum.music.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * Represents a content provider for the music application.
 * <p>
 * An audio source identifies where the catalog and streams come from,
 * such as local storage or a remote streaming service.
 * </p>
 *
 * @param name the unique code of the source (e.g., "local", "tdl")
 */
@Schema(name = "AudioSource", description = "Content provider for the music catalog and streams")
public record AudioSource(@Schema String id, @Schema String name, List<String> capabilities) {
}
