package quantum.music.dto.detail;

import quantum.music.dto.summary.Artist;

public record AlbumDetail(
        Long id,
        String title,
        Artist artist,
        String release,
        String copyright,
        String type,
        String cover,
        String tracks) {
}