package quantum.music.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import quantum.music.domain.local.QAlbum;

/**
 * Reactive repository for {@link quantum.music.domain.local.QAlbum} documents.
 * Provides CRUD access to the local albums collection via Panache.
 */
@ApplicationScoped
public class AlbumRepository implements ReactivePanacheMongoRepository<QAlbum> {
}