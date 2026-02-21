package quantum.music.providers.tdl;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import quantum.music.domain.PagedResponse;
import quantum.music.domain.providers.*;
import quantum.music.providers.MusicProvider;
import quantum.music.providers.tdl.services.TdlArtistService;
import quantum.music.providers.tdl.services.TdlTrackService;
import quantum.music.providers.tdl.services.TldAlbumService;
import quantum.music.providers.tdl.services.TldSearchService;

import java.util.List;

@ApplicationScoped
public class TdlMusicProvider implements MusicProvider {

    private static final Logger LOG = Logger.getLogger(TdlMusicProvider.class);

    @ConfigProperty(name = "tdl.provider.name", defaultValue = "The Digital Library")
    private String providerName;

    @Inject
    private TldSearchService searchService;

    @Inject
    private TldAlbumService albumService;

    @Inject
    private TdlTrackService trackService;

    @Inject
    private TdlArtistService artistService;

    @Override
    public String getProviderId() {
        return "tdl";
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public List<String> getCapabilities() {
        return List.of("list", "play", "import");
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
        return trackService.content(trackId, codec, quality, presentation)
                .onItem().transformToMulti( stream -> trackService.streamFile(stream));
    }
}
