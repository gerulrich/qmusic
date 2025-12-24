package quantum.music.providers.tdl;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import quantum.music.domain.PagedResponse;
import quantum.music.domain.providers.Album;
import quantum.music.domain.providers.Track;
import quantum.music.providers.MusicProvider;

@ApplicationScoped
public class TdlMusicProvider implements MusicProvider {

    private static final Logger LOG = Logger.getLogger(TdlMusicProvider.class);

    @Inject
    private TldSearchService searchService;

    @Inject
    private TldAlbumService albumService;

    @Inject
    private TdlTrackService trackService;

    @Override
    public Uni<PagedResponse<Album>> search(String q, int offset, int limit) {
        return searchService.search(q, offset, limit);
    }

    @Override
    public Uni<PagedResponse<Album>> getAlbumsByArtistId(String artistId, int offset, int limit) {
        return albumService.getAlbumsByArtistId(artistId, offset, limit);
    }

    @Override
    public Uni<Album> getAlbumById(String albumId) {
        return albumService.getAlbumById(albumId);
    }

    @Override
    public Uni<Album> getTracksByAlbumId(String albumId) {
        return albumService.getTracksByAlbumId(albumId);
    }

    @Override
    public Uni<Track> getTrackById(String trackId) {
        return trackService.getTrackById(trackId);
    }

    @Override
    public Multi<Buffer> streamTrackById(String trackId, String codec, String quality, String presentation) {
        return trackService.content(trackId, codec, quality, presentation)
                .onItem().transformToMulti( stream -> trackService.streamFile(stream));
    }
}
