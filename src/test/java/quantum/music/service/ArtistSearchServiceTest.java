package quantum.music.service;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import quantum.music.domain.local.QArtist;
import quantum.music.repository.ArtistRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ArtistSearchServiceTest {

    @Test
    void generatesSearchEmbeddingAndPassesVectorToArtistRepository() {
        ArtistEmbeddingService embeddingService = mock();
        ArtistRepository artistRepository = mock();
        ArtistSearchService service = new ArtistSearchService();
        service.artistEmbeddingService = embeddingService;
        service.artistRepository = artistRepository;
        when(embeddingService.embedQuery("SERAFIN")).thenReturn(Uni.createFrom().item(List.of(0.25, -0.5)));
        when(artistRepository.vectorSearch(List.of(0.25, -0.5)))
            .thenReturn(Uni.createFrom().item(List.of()));

        service.search("SERAFIN").await().indefinitely();

        verify(embeddingService).embedQuery("SERAFIN");
        verify(artistRepository).vectorSearch(List.of(0.25, -0.5));
    }

    @Test
    void blankQuerySkipsEmbeddingAndDatabaseSearch() {
        ArtistEmbeddingService embeddingService = mock();
        ArtistRepository artistRepository = mock();
        ArtistSearchService service = new ArtistSearchService();
        service.artistEmbeddingService = embeddingService;
        service.artistRepository = artistRepository;

        assertEquals(List.of(), service.search("  ").await().indefinitely());
        verifyNoInteractions(embeddingService, artistRepository);
    }

    @Test
    void returnsEveryArtistFoundByVectorSearch() {
        ArtistEmbeddingService embeddingService = mock();
        ArtistRepository artistRepository = mock();
        ArtistSearchService service = new ArtistSearchService();
        service.artistEmbeddingService = embeddingService;
        service.artistRepository = artistRepository;
        QArtist artist = new QArtist();
        artist.name = "Serafin";
        QArtist anotherArtist = new QArtist();
        anotherArtist.name = "Los Manseros";
        when(embeddingService.embedQuery("Serafin")).thenReturn(Uni.createFrom().item(List.of(0.25)));
        when(artistRepository.vectorSearch(List.of(0.25)))
            .thenReturn(Uni.createFrom().item(List.of(artist, anotherArtist)));

        assertEquals(List.of(artist, anotherArtist), service.search("Serafin").await().indefinitely());
    }
}
