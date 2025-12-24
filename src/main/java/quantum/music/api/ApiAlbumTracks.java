package quantum.music.api;

import quantum.music.domain.providers.Album;

import java.util.List;

public record ApiAlbumTracks(
        Album album,
        int total,
        List<ApiTrack> tracks
        ) {
}
