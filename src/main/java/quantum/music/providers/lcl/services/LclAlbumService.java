package quantum.music.providers.lcl.services;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import quantum.music.domain.PagedResponse;
import quantum.music.domain.local.QAlbum;
import quantum.music.domain.local.QTrack;
import quantum.music.domain.local.QSource;
import quantum.music.domain.providers.Album;
import quantum.music.domain.providers.Artist;
import quantum.music.domain.providers.Track;
import quantum.music.domain.providers.TrackList;
import quantum.music.repository.AlbumRepository;

/**
 * Service for local album lookups and mappings.
 *
 * <p>Provides provider-facing retrieval and mapping logic for albums and tracks.</p>
 */
@ApplicationScoped
public class LclAlbumService extends LclProviderService {

    private static final Logger LOG = Logger.getLogger(LclAlbumService.class);

    @Inject
    AlbumRepository repository;

    /**
     * Retrieves local albums for an artist with pagination.
     *
     * @param artistId provider-facing artist id
     * @param offset zero-based item offset
     * @param limit maximum number of items to return
     * @return paged list of albums for the artist
     */
    public Uni<PagedResponse<Album>> getAlbumsByArtistId(String artistId, int offset, int limit) {
        LOG.debugf("Fetching local albums for artistId=%s, offset=%d, limit=%d", artistId, offset, limit);
        ObjectId id = new ObjectId(parsedId(artistId));
        int page = pageIndex(offset, limit);
        return Uni.combine().all().unis(
                repository.find("artistId", id).page(page, limit).list(),
                repository.count("artistId", id)
            )
            .asTuple()
            .onItem().transform(tuple -> new PagedResponse<>(
                        mapList(tuple.getItem1(), this::map),
                        offset,
                        limit,
                        tuple.getItem2().intValue()
        ));
    }

    /**
     * Retrieves a local album by its provider-facing id.
     *
     * @param albumId provider-facing album id
     * @return album data for the requested id
     * @throws NotFoundException when the album does not exist
     */
    public Uni<Album> getAlbumById(String albumId) {
        LOG.debugf("Fetching local album details for id=%s", albumId);
        return repository.findById(new ObjectId(parsedId(albumId)))
            .onItem().ifNull().failWith(() -> new NotFoundException(STR."Album not found: \{albumId}"))
            .onItem().transform(this::map);
    }

    /**
     * Retrieves tracks for a local album by provider-facing id.
     *
     * @param albumId provider-facing album id
     * @return album data with its track list
     * @throws NotFoundException when the album does not exist
     */
    public Uni<TrackList> getTracksByAlbumId(String albumId) {
        LOG.debugf("Fetching local album tracks for id=%s", albumId);
        return repository.findById(new ObjectId(parsedId(albumId)))
            .onItem().ifNull().failWith(() -> new NotFoundException(STR."Album not found: \{albumId}"))
            .onItem().transform(album -> new TrackList(map(album), mapList(album.tracks, track -> map(track, album))));
    }

    /** Maps a local album entity into a provider album DTO. */
    private Album map(QAlbum album) {
        return Album.builder()
            .id(formatId(album.id))
            .title(album.title)
            .artist(Artist
                    .builder()
                    .id(formatId(album.artistId))
                    .name(album.artist)
                    .build()
            )
            .release(album.release)
            .copyright(album.copyright)
            .type("ALBUM")
            .cover(album.cover)
            .tags(sourceTags(album.source))
            .build();
    }

    /** Maps a local track entity into a provider track DTO. */
    private Track map(QTrack track, QAlbum album) {
        QSource source = album.source;
        return Track.builder()
            .id(formatId(track._id))
            .title(track.title)
            .duration(track.duration)
            .trackNumber(track.trackNumber)
            .volumeNumber(track.discNumber)
            .codec(source != null ? source.format : null)
            .quality(source != null ? source.quality : null)
            .tags(sourceTags(source))
            .build();
    }

}
