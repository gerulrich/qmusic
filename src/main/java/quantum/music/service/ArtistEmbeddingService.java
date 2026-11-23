package quantum.music.service;

import dev.langchain4j.model.embedding.EmbeddingModel;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import quantum.music.domain.local.QArtist;
import quantum.music.repository.ArtistRepository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/// Generates and persists embeddings for artists and search queries.
@ApplicationScoped
public class ArtistEmbeddingService {

    private static final Logger LOG = Logger.getLogger(ArtistEmbeddingService.class);
    private static final String QUERY_PREFIX = "Represent this sentence for searching relevant passages: ";

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    ArtistRepository artistRepository;

    /// Embeds and persists a newly created artist.
    public Uni<QArtist> embedAndPersist(QArtist artist) {
        return embed(artistText(artist))
            .onItem().transformToUni(vector -> {
                artist.embedding = vector;
                return artistRepository.persist(artist);
            });
    }

    /// Embeds artists without vectors in sequence, persisting each vector before moving on.
    public Uni<Long> embedMissingArtists() {
        return artistRepository.findAll()
                .stream()
        //return artistRepository.findWithoutEmbedding()
            .onItem().transformToUniAndConcatenate(artist ->
                embed(artistText(artist))
                    .onItem().transformToUni(vector -> artistRepository.updateEmbedding(artist.id, vector))
                    .replaceWith(1L)
            )
            .collect().with(Collectors.summingLong(Long::longValue));
    }

    /// Generates a query vector using the BGE-recommended retrieval prefix.
    public Uni<List<Double>> embedQuery(String query) {
        return embed(QUERY_PREFIX + query);
    }

    private Uni<List<Double>> embed(String text) {
        return Uni.createFrom()
            .item(() -> Arrays.stream(embeddingModel.embed(text).content().vectorAsDoubleArray()).boxed().toList())
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool());
    }

    private static String artistText(QArtist artist) {
        String text = "Artist: " + artist.name;
        if (artist.bio != null && !artist.bio.isBlank()) {
            text += ". " + artist.bio;
        }
        return text;
    }
}
