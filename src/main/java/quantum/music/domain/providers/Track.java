package quantum.music.domain.providers;

import java.util.List;

public record Track(
        String id,
        String title,
        int duration,
        int trackNumber,
        int volumeNumber,
        String codec,
        List<String> tags,
        String version,
        String copyright,
        String link,
        String content) {

    public Track(String id, String title, int duration, int trackNumber, int volumeNumber, String link) {
        this(id, title, duration, trackNumber, volumeNumber, null, null, null, null, link, null);
    }
}
