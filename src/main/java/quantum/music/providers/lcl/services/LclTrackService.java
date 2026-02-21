package quantum.music.providers.lcl.services;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.file.OpenOptions;
import io.vertx.mutiny.core.file.AsyncFile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import quantum.music.domain.local.QAlbum;
import quantum.music.domain.local.QSource;
import quantum.music.domain.local.QTrack;
import quantum.music.domain.providers.Album;
import quantum.music.domain.providers.Artist;
import quantum.music.domain.providers.Track;
import quantum.music.domain.providers.TrackDetail;
import quantum.music.repository.AlbumRepository;

import io.vertx.mutiny.core.buffer.Buffer;

import io.vertx.mutiny.core.Vertx;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import java.util.Optional;

/**
 * Service responsible for fetching a single track from local albums.
 *
 * <p>Performs a lookup by nested track id and maps album/track data into
 * provider DTOs. The response includes minimal album info and track
 * metadata derived from the album source (codec and quality).</p>
 */
@ApplicationScoped
public class LclTrackService extends LclProviderService {

    private static final Logger LOG = Logger.getLogger(LclTrackService.class);

    @Inject
    Vertx vertx;

    @Inject
    AlbumRepository repository;

    /**
     * Retrieves a track by its provider-facing id.
     *
     * @param trackId provider-facing track id
     * @return track detail for the requested id
     * @throws NotFoundException when the track does not exist
     */
    public Uni<TrackDetail> getTrackById(String trackId) {
        ObjectId id = new ObjectId(parsedId(trackId));
        LOG.debugf("LCL track lookup started: trackId=%s", trackId);
        return repository.find("{ 'tracks._id': ?1 }", id).firstResult()
                .onItem().ifNull().failWith(() -> new NotFoundException(STR."Track not found: \{trackId}"))
                .onItem().transform(album -> {
                    LOG.debugf("LCL track album hit: trackId=%s, albumId=%s", trackId, formatId(album.id));
                    return Optional.ofNullable(album.tracks).orElse(Collections.emptyList())
                            .stream()
                            .filter(track -> id.equals(track._id))
                            .findFirst()
                            .map(track -> map(album, track, album.source))
                            .orElseThrow(() -> new NotFoundException(STR."Track not found: \{trackId}"));
                });
    }

    public Multi<Buffer> streamTrackById(String trackId, String codec, String quality, String presentation) {
        ObjectId id = new ObjectId(parsedId(trackId));
        LOG.infof("LCL track stream lookup started: trackId=%s", trackId);
        return repository.find("{ 'tracks._id': ?1 }", id).firstResult()
                .onItem().ifNull().failWith(() -> new NotFoundException(STR."Track not found: \{trackId}"))
                .onItem().transform(album -> {
                    LOG.debugf("LCL track album hit: trackId=%s, albumId=%s", trackId, formatId(album.id));
                    return Optional.ofNullable(album.tracks).orElse(Collections.emptyList())
                            .stream()
                            .filter(track -> id.equals(track._id))
                            .findFirst()
                            .map(track -> track.filePath)
                            .filter(filePath -> !filePath.isBlank())
                            .orElseThrow(() -> new NotFoundException(STR."Track not found: \{trackId}"));
                }).onItem().transformToMulti(this::streamFile);
    }

    private Multi<Buffer> streamFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new NotFoundException("Track file not found");
        }
        Path file = Paths.get(filePath); // TODO agregar base path
        if (!Files.exists(file) || !Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new NotFoundException("Track file not found");
        }
        return vertx.fileSystem()
                .open(file.toString(), new OpenOptions().setRead(true))
                .onItem().transformToMulti(AsyncFile::toMulti);
    }

    /** Maps local album/track entities into a provider track detail DTO. */
    private TrackDetail map(QAlbum album, QTrack track, QSource source) {
        return new TrackDetail(
            Album.builder()
                .id(formatId(album.id))
                .title(album.title)
                .release(album.release)
                .artist(Artist.builder().id(formatId(album.artistId)).name(album.artist).build())
                .cover(album.cover)
            .build(),
            Track.builder()
                .id(formatId(track._id))
                .title(track.title)
                .duration(track.duration)
                .trackNumber(track.trackNumber)
                .volumeNumber(track.discNumber)
                .codec(source != null ? source.format : null)
                .quality(source != null ? source.quality : null)
                .tags(sourceTags(source))
            .build()
        );
    }

}
