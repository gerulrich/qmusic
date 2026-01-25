package quantum.music.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import quantum.music.domain.local.QArtist;

/**
 * Reactive repository for {@link quantum.music.domain.local.QArtist} documents.
 * Provides CRUD access to the local artists collection via Panache.
 */
@ApplicationScoped
public class ArtistRepository implements ReactivePanacheMongoRepository<QArtist> {
}