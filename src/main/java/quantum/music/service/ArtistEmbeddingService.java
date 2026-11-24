package quantum.music.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import quantum.music.client.OpenAiEmbeddingClient;
import quantum.music.client.OpenAiEmbeddingClient.EmbeddingRequest;
import quantum.music.client.OpenAiEmbeddingClient.EmbeddingResponse;
import quantum.music.domain.local.QArtist;
import quantum.music.repository.ArtistRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/// Generates and persists embeddings for artists and search queries.
@ApplicationScoped
public class ArtistEmbeddingService {

    private static final String MODEL = "text-embedding-3-small";
    private static final int DIMENSIONS = 384;

    @Inject
    @RestClient
    OpenAiEmbeddingClient embeddingClient;

    @ConfigProperty(name = "openai.api-key")
    Optional<String> apiKey;

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
        return artistRepository.findWithoutEmbedding()
            .onItem().transformToUniAndConcatenate(artist ->
                embed(artistText(artist))
                    .onItem().transformToUni(vector -> artistRepository.updateEmbedding(artist.id, vector))
                    .replaceWith(1L)
            )
            .collect().with(Collectors.summingLong(Long::longValue));
    }

    /// Generates a query vector using the same model as the artist vectors.
    public Uni<List<Double>> embedQuery(String query) {
        return embed(query);
    }

    private Uni<List<Double>> embed(String text) {
        if (apiKey.isEmpty() || apiKey.get().isBlank()) {
            return Uni.createFrom().failure(new IllegalStateException("OPENAI_API_KEY must be configured to generate embeddings"));
        }
        return embeddingClient.embed("Bearer " + apiKey.get(), new EmbeddingRequest(MODEL, text, DIMENSIONS, "float"))
            .onItem().transform(ArtistEmbeddingService::extractVector);
    }

    private static List<Double> extractVector(EmbeddingResponse response) {
        if (response == null || response.data() == null || response.data().size() != 1
            || response.data().getFirst() == null || response.data().getFirst().index() != 0
            || response.data().getFirst().embedding() == null
            || response.data().getFirst().embedding().size() != DIMENSIONS
            || response.data().getFirst().embedding().stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
            throw new IllegalStateException("OpenAI returned an invalid embedding; expected one 384-dimensional vector");
        }
        return response.data().getFirst().embedding();
    }

    private static String artistText(QArtist artist) {
        String text = "Artist: " + artist.name;
        if (artist.bio != null && !artist.bio.isBlank()) {
            text += ". " + artist.bio;
        }
        return text;
    }
}
