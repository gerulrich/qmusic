package quantum.music.dto.summary;

public record Track(
        Long id,
        String title,
        int duration,
        int track,
        int volume,
        Album album,
        String link
) {

}
