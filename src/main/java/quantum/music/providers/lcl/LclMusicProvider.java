package quantum.music.providers.lcl;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import quantum.music.domain.PagedResponse;
import quantum.music.domain.providers.*;
import quantum.music.providers.MusicProvider;
import quantum.music.providers.lcl.services.LclAlbumService;
import quantum.music.providers.lcl.services.LclArtistService;
import quantum.music.providers.lcl.services.LclSearchService;
import quantum.music.providers.lcl.services.LclTrackService;
import quantum.music.repository.AlbumRepository;

import java.util.List;

@ApplicationScoped
public class LclMusicProvider implements MusicProvider {

    @Inject
    AlbumRepository repository;

    @Inject
    private LclSearchService searchService;

    @Inject
    private LclAlbumService albumService;

    @Inject
    private LclTrackService trackService;

    @Inject
    private LclArtistService artistService;

    @Override
    public String getProviderId() {
        return "lcl";
    }

    @Override
    public String getProviderName() {
        return "LOCAL";
    }

    @Override
    public List<String> getCapabilities() {
        return List.of("list", "play", "edit");
    }

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
    public Uni<TrackList> getTracksByAlbumId(String albumId) {
        return albumService.getTracksByAlbumId(albumId);
    }

    @Override
    public Uni<TrackDetail> getTrackById(String trackId) {
       return trackService.getTrackById(trackId);
    }

    @Override
    public Uni<Artist> getArtistById(String artistId) {
        return artistService.getArtistById(artistId);
    }

    @Override
    public Multi<Buffer> streamTrackById(String trackId, String codec, String quality, String presentation) {
        return trackService.streamTrackById(trackId, codec, quality, presentation);
    }


}
