package quantum.music.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import quantum.music.domain.local.QArtist;
import quantum.music.repository.ArtistRepository;

import java.util.List;

/// Performs semantic artist searches using locally generated embeddings.
@ApplicationScoped
public class ArtistSearchService {

    @Inject
    ArtistEmbeddingService artistEmbeddingService;

    @Inject
    ArtistRepository artistRepository;

    /// Searches imported artists by semantic similarity to the supplied query.
    public Uni<List<QArtist>> search(String query) {
        if (query == null || query.isBlank()) {
            return Uni.createFrom().item(List.of());
        }

        return artistEmbeddingService.embedQuery(query)
            .onItem().transformToUni(artistRepository::vectorSearch);
    }
}
