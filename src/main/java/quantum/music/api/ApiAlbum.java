package quantum.music.api;

import java.util.List;

public record ApiAlbum(
        String id,
        String title,
        String release,
        ApiArtist artist,
        String cover,
        String copyright,
        String tracks, // -> en modo detail, el link a los tracks
        List<String> tags,
        String link,
        String type
) {

    public ApiAlbum(String id, String title, String release, ApiArtist artist, String cover, String copyright, List<String> tags, String link, String type) {
        this(id, title, release, artist, cover, copyright, null, tags, link, type);
    }

    public ApiAlbum(String id, String title, String release, ApiArtist artist, String cover, String copyright, String tracks, List<String> tags, String type) {
        this(id, title, release, artist, cover, copyright, tracks, tags, null, type);
    }
}
