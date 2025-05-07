package quantum.music.dto.detail;

import quantum.music.dto.summary.Album;

import java.util.List;

public record TrackDetail(
        Long id,
        String title,
        int duration,
        int track,
        int volume,
        String codec,
        List<String> tags,
        String version,
        String copyright,
        Album album,
        String content
) {
}
