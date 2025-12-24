package quantum.music.providers.lcl;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import quantum.music.domain.PagedResponse;
import quantum.music.domain.providers.Album;
import quantum.music.domain.providers.Track;
import quantum.music.providers.MusicProvider;

@ApplicationScoped
public class LclMusicProvider implements MusicProvider {

    @Override
    public Uni<PagedResponse<Album>> search(String q, int offset, int limit) {
        return Uni.createFrom().failure(new UnsupportedOperationException("Search not supported in LCL provider"));
    }

    @Override
    public Uni<PagedResponse<Album>> getAlbumsByArtistId(String artistId, int offset, int limit) {
        return null;
    }

    @Override
    public Uni<Album> getAlbumById(String album) {
        return Uni.createFrom().failure(new UnsupportedOperationException("Search not supported in LCL provider"));
    }

    @Override
    public Uni<Album> getTracksByAlbumId(String album) {
        return Uni.createFrom().failure(new UnsupportedOperationException("Search not supported in LCL provider"));
    }

    @Override
    public Uni<Track> getTrackById(String trackId) {
        return Uni.createFrom().failure(new UnsupportedOperationException("Search not supported in LCL provider"));
    }

    @Override
    public Multi<Buffer> streamTrackById(String id, String codec, String quality, String presentation) {
        return null;
    }


}
