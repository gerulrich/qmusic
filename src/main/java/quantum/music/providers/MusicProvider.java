package quantum.music.providers;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import quantum.music.domain.providers.Album;
import quantum.music.domain.PagedResponse;
import quantum.music.domain.providers.Track;

public interface MusicProvider {

    Uni<PagedResponse<Album>> search(String q, int offset, int limit);

    Uni<PagedResponse<Album>> getAlbumsByArtistId(String artistId, int offset, int limit);

    Uni<Album> getAlbumById(String albumId);

    Uni<Album> getTracksByAlbumId(String albumId);

    Uni<Track> getTrackById(String trackId);

    Multi<Buffer> streamTrackById(String trackId, String codec, String quality, String presentation);
}
