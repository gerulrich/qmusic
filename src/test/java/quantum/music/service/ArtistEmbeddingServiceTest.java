package quantum.music.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import quantum.music.domain.local.QArtist;
import quantum.music.repository.ArtistRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArtistEmbeddingServiceTest {

    @Test
    void embedsSearchQueryWithBgeSearchPrefix() {
        ArtistEmbeddingService service = new ArtistEmbeddingService();
        service.embeddingModel = modelReturning(new float[]{0.25f, -0.5f});

        assertEquals(
            List.of(0.25, -0.5),
            service.embedQuery("SERAFIN").await().indefinitely()
        );

        verify(service.embeddingModel)
            .embed("Represent this sentence for searching relevant passages: SERAFIN");
    }

    @Test
    void generatesAndPersistsMissingArtistEmbeddings() {
        ArtistRepository repository = mock();
        EmbeddingModel model = modelReturning(new float[]{0.25f, -0.5f});
        ArtistEmbeddingService service = new ArtistEmbeddingService();
        service.artistRepository = repository;
        service.embeddingModel = model;
        QArtist artist = new QArtist();
        artist.id = new ObjectId();
        artist.name = "Serafin";
        artist.bio = "Argentine musician";
        when(repository.findWithoutEmbedding()).thenReturn(Multi.createFrom().item(artist));
        when(repository.updateEmbedding(artist.id, List.of(0.25, -0.5)))
            .thenReturn(Uni.createFrom().voidItem());

        assertEquals(1L, service.embedMissingArtists().await().indefinitely());

        verify(model).embed("Artist: Serafin. Argentine musician");
        verify(repository).updateEmbedding(artist.id, List.of(0.25, -0.5));
    }

    @Test
    void embedsNewArtistBeforePersisting() {
        ArtistRepository repository = mock();
        EmbeddingModel model = modelReturning(new float[]{0.75f});
        ArtistEmbeddingService service = new ArtistEmbeddingService();
        service.artistRepository = repository;
        service.embeddingModel = model;
        QArtist artist = new QArtist();
        artist.name = "Serafin";
        when(repository.persist(artist)).thenReturn(Uni.createFrom().item(artist));

        assertEquals(artist, service.embedAndPersist(artist).await().indefinitely());
        assertEquals(List.of(0.75), artist.embedding);
        verify(model).embed("Artist: Serafin");
        verify(repository).persist(artist);
    }

    private static EmbeddingModel modelReturning(float[] vector) {
        EmbeddingModel model = mock();
        @SuppressWarnings("unchecked")
        Response<Embedding> response = mock();
        when(response.content()).thenReturn(new Embedding(vector));
        when(model.embed(org.mockito.ArgumentMatchers.anyString())).thenReturn(response);
        return model;
    }
}
