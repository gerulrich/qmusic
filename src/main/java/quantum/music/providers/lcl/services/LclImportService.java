package quantum.music.providers.lcl.services;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.bson.types.ObjectId;
import quantum.music.domain.imports.AlbumImportMessage;
import quantum.music.domain.local.QAlbum;
import quantum.music.domain.local.QArtist;
import quantum.music.domain.local.QSource;
import quantum.music.domain.local.QTrack;
import quantum.music.domain.providers.Album;
import quantum.music.domain.providers.Track;
import quantum.music.repository.AlbumRepository;
import quantum.music.repository.ArtistRepository;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Consumes album import messages and persists the metadata locally.
 */
@ApplicationScoped
public class LclImportService {

    private static final Logger LOG = Logger.getLogger(LclImportService.class);

    @Inject
    AlbumRepository albumRepository;

    @Inject
    ArtistRepository artistRepository;

    @ConsumeEvent("album-import")
    public Uni<Void> onAlbumImport(AlbumImportMessage message) {
        if (message == null || message.trackList() == null || message.trackList().album() == null) {
            LOG.warn("Album import message missing album payload");
            return Uni.createFrom().voidItem();
        }
        Album album = message.trackList().album();
        String albumId = album.id();
        return albumRepository.find("source._id", albumId).firstResult()
                .onItem().transformToUni(existing -> {
                    if (existing != null) {
                        LOG.infof("Album already imported: %s", albumId);
                        return Uni.createFrom().voidItem();
                    }
                    return upsertArtist(album)
                            .onItem().transformToUni(artist -> albumRepository.persist(mapAlbum(message, album, artist)).replaceWithVoid());
                });
    }

    private Uni<QArtist> upsertArtist(Album album) {
        if (album.artist() == null || album.artist().name() == null || album.artist().name().isBlank()) {
            return Uni.createFrom().item((QArtist) null);
        }
        String artistName = album.artist().name().trim();
        String normalized = normalizeName(artistName);
        return artistRepository.find(
                        "{ '$or': [ { 'name_normalized': ?1 }, { 'name': ?2 }, { 'name_variations': ?2 } ] }",
                        normalized,
                        artistName)
                .firstResult()
                .onItem().transformToUni(existing -> {
                    if (existing != null) {
                        return Uni.createFrom().item(existing);
                    }
                    QArtist artist = new QArtist();
                    artist.name = artistName;
                    artist.bio = album.artist().bio();
                    artist.nameNormalized = normalized;
                    artist.nameVariations = List.of(artistName);
                    return artistRepository.persist(artist);
                });
    }

    private static String normalizeName(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private QAlbum mapAlbum(AlbumImportMessage message, Album album, QArtist artist) {
        QAlbum entity = new QAlbum();
        entity.title = album.title();
        entity.artist = artist != null ? artist.name : (album.artist() != null ? album.artist().name() : null);
        entity.albumArtist = entity.artist;
        entity.artistId = artist != null ? artist.id : null;
        entity.release = album.release();
        entity.copyright = album.copyright();
        entity.cover = album.cover();
        entity.source = mapSource(message.sourceProviderId(), album);
        entity.tracks = mapTracks(entity.artist, message.trackList().tracks());
        return entity;
    }

    private QSource mapSource(String providerId, Album album) {
        QSource source = new QSource();
        source.id = album.id();
        source.name = providerId;
        source.type = providerId;
        source.status = "IMPORTED";
        List<String> tags = album.tags();
        if (tags != null && !tags.isEmpty()) {
            source.format = tags.get(0);
            if (tags.size() > 1) {
                source.quality = tags.get(1);
            }
        }
        return source;
    }

    private List<QTrack> mapTracks(String artistName, List<Track> tracks) {
        if (tracks == null || tracks.isEmpty()) {
            return List.of();
        }
        List<QTrack> mapped = new ArrayList<>(tracks.size());
        for (Track track : tracks) {
            QTrack entity = new QTrack();
            entity._id = new ObjectId();
            entity.title = track.title();
            entity.artist = artistName;
            entity.trackNumber = track.trackNumber();
            entity.discNumber = track.volumeNumber();
            entity.duration = track.duration();
            entity.sourceId = track.id();
            mapped.add(entity);
        }
        return mapped;
    }
}
