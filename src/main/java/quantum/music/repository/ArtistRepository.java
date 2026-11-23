package quantum.music.repository;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheMongoRepository;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.bson.Document;
import org.bson.types.ObjectId;
import quantum.music.domain.local.QArtist;

import java.util.List;

/// Reactive repository for {@link quantum.music.domain.local.QArtist} documents.
/// Provides CRUD access to the local artists collection via Panache.
@ApplicationScoped
public class ArtistRepository implements ReactivePanacheMongoRepository<QArtist> {

    private static final String VECTOR_INDEX = "artists_vector_index";
    private static final int VECTOR_CANDIDATES = 100;
    private static final int VECTOR_RESULT_LIMIT = 10;

    /// Streams artists that have not yet been assigned an embedding.
    public Multi<QArtist> findWithoutEmbedding() {
        return find("{ '$or': [ { 'embedding': { '$exists': false } }, { 'embedding': null } ] }").stream();
    }

    /// Stores the embedding for an existing artist.
    public Uni<Void> updateEmbedding(ObjectId artistId, List<Double> embedding) {
        return mongoCollection()
            .updateOne(
                new Document("_id", artistId),
                new Document("$set", new Document("embedding", embedding))
            )
            .replaceWithVoid();
    }

    /// Finds artists nearest to the supplied embedding.
    public Uni<List<QArtist>> vectorSearch(List<Double> queryVector) {
        Document vectorSearch = new Document("index", VECTOR_INDEX)
            .append("path", "embedding")
            .append("queryVector", queryVector)
            .append("numCandidates", VECTOR_CANDIDATES)
            .append("limit", VECTOR_RESULT_LIMIT);

        return mongoCollection()
            .aggregate(List.of(new Document("$vectorSearch", vectorSearch)))
            .collect().asList();
    }
}