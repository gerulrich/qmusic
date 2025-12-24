package quantum.music.api;

import java.util.List;

public record ApiTrack(
        String id,
        String title,
        int duration,
        int track,
        int volume,
        ApiAlbum album,
        String codec,
        List<String> tags,
        String version,
        String copyright,
        String link,
        String content,
        List<ApiTrackStream> streams) {

    public ApiTrack(String id, String title, int duration, int track, int volume, ApiAlbum album, String link) {
        this(id, title, duration, track, volume, album, null, null, null, null, link, null, null);
    }
}