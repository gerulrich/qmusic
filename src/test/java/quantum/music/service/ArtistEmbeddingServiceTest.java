package quantum.music.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import quantum.music.client.OpenAiEmbeddingClient;
import quantum.music.client.OpenAiEmbeddingClient.EmbeddingData;
import quantum.music.client.OpenAiEmbeddingClient.EmbeddingRequest;
import quantum.music.client.OpenAiEmbeddingClient.EmbeddingResponse;
import quantum.music.domain.local.QArtist;
import quantum.music.repository.ArtistRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ArtistEmbeddingServiceTest {

    private static final List<Double> VECTOR = Collections.nCopies(384, 0.25);

    @Test
    void embedsSearchQueryWithOpenAi() {
        ArtistEmbeddingService service = service();

        assertEquals(VECTOR, service.embedQuery("SERAFIN").await().indefinitely());
        verify(service.embeddingClient).embed("Bearer test-key",
            new EmbeddingRequest("text-embedding-3-small", "SERAFIN", 384, "float"));
    }

    @Test
    void generatesAndPersistsMissingArtistEmbeddings() {
        ArtistRepository repository = mock();
        ArtistEmbeddingService service = service();
        service.artistRepository = repository;
        QArtist artist = new QArtist();
        artist.id = new ObjectId();
        artist.name = "Serafin";
        artist.bio = "Argentine musician";
        when(repository.findWithoutEmbedding()).thenReturn(Multi.createFrom().item(artist));
        when(repository.updateEmbedding(artist.id, VECTOR)).thenReturn(Uni.createFrom().voidItem());

        assertEquals(1L, service.embedMissingArtists().await().indefinitely());
        verify(service.embeddingClient).embed("Bearer test-key",
            new EmbeddingRequest("text-embedding-3-small", "Artist: Serafin. Argentine musician", 384, "float"));
        verify(repository).updateEmbedding(artist.id, VECTOR);
    }

    @Test
    void embedsNewArtistBeforePersisting() {
        ArtistRepository repository = mock();
        ArtistEmbeddingService service = service();
        service.artistRepository = repository;
        QArtist artist = new QArtist();
        artist.name = "Serafin";
        when(repository.persist(artist)).thenReturn(Uni.createFrom().item(artist));

        assertEquals(artist, service.embedAndPersist(artist).await().indefinitely());
        assertEquals(VECTOR, artist.embedding);
        verify(service.embeddingClient).embed("Bearer test-key",
            new EmbeddingRequest("text-embedding-3-small", "Artist: Serafin", 384, "float"));
        verify(repository).persist(artist);
    }

    @Test
    void rejectsMissingApiKey() {
        ArtistEmbeddingService service = service();
        service.apiKey = Optional.empty();

        assertThrows(IllegalStateException.class, () -> service.embedQuery("artist").await().indefinitely());
        verifyNoInteractions(service.embeddingClient);
    }

    @Test
    void rejectsInvalidVector() {
        ArtistEmbeddingService service = service();
        when(service.embeddingClient.embed(anyString(), any()))
            .thenReturn(Uni.createFrom().item(new EmbeddingResponse(List.of(new EmbeddingData(List.of(0.5), 0)))));

        assertThrows(IllegalStateException.class, () -> service.embedQuery("artist").await().indefinitely());
    }

    @Test
    void propagatesApiFailures() {
        ArtistEmbeddingService service = service();
        when(service.embeddingClient.embed(anyString(), any()))
            .thenReturn(Uni.createFrom().failure(new IllegalStateException("API failed")));

        assertEquals("API failed", assertThrows(IllegalStateException.class,
            () -> service.embedQuery("artist").await().indefinitely()).getMessage());
    }

    private static ArtistEmbeddingService service() {
        ArtistEmbeddingService service = new ArtistEmbeddingService();
        service.apiKey = Optional.of("test-key");
        service.embeddingClient = mock();
        when(service.embeddingClient.embed(anyString(), any()))
            .thenReturn(Uni.createFrom().item(new EmbeddingResponse(List.of(new EmbeddingData(VECTOR, 0)))));
        return service;
    }
}
