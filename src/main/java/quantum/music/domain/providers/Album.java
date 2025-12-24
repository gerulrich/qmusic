package quantum.music.domain.providers;

import java.util.List;

public record Album(
        String id,
        String title,
        Artist artist,
        String release,
        String copyright,
        String type,
        String cover,
        List<Track> tracks,
        String link) {

    public Album(String id, String title, Artist artist, String link) {
        this(id, title, artist, null, null, null, null, null, link);
    }
}