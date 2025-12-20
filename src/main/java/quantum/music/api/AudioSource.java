package quantum.music.api;

/**
 * Represents an audio source for the music application.
 * <p>
 * An audio source defines where music content can be retrieved from,
 * such as local storage or remote streaming services.
 * </p>
 *
 * @param name the unique name identifier of the audio source
 */
public record AudioSource(String name) {}
