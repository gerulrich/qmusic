package quantum.music.dto.detail;

import quantum.music.dto.summary.Album;

public record TrackDetail(
        Long id,
        String title,
        Album album,
        String content
) {
}
