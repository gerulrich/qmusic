package quantum.music.providers;

import io.quarkus.mongodb.panache.reactive.ReactivePanacheQuery;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import quantum.music.domain.imports.AlbumImportMessage;
import quantum.music.domain.local.QAlbum;
import quantum.music.domain.providers.Album;
import quantum.music.domain.providers.TrackList;
import quantum.music.providers.tdl.TdlMusicProvider;
import quantum.music.providers.tdl.services.TldAlbumService;
import quantum.music.repository.AlbumRepository;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
class AlbumImportMessagingTest {

    @Inject
    TdlMusicProvider provider;

    @Inject
    @Channel("album-import")
    MutinyEmitter<AlbumImportMessage> imports;

    @InjectMock
    TldAlbumService albumService;

    @InjectMock
    AlbumRepository repository;

    @BeforeEach
    void setUp() {
        ReactivePanacheQuery<QAlbum> query = mock();
        doReturn(query).when(repository).find(anyString(), any(Object[].class));
        when(query.firstResult()).thenReturn(Uni.createFrom().nullItem());
        Album album = Album.builder().id("tdl:123").title("Test album").build();
        when(albumService.getAlbumById("tdl:123")).thenReturn(Uni.createFrom().item(album));
        when(albumService.getTracksByAlbumId("tdl:123"))
                .thenReturn(Uni.createFrom().item(new TrackList(album, List.of())));
    }

    @Test
    void deliversToLocalConsumerAndWaitsForPersistence() {
        when(repository.persist(any(QAlbum.class)))
                .thenAnswer(invocation -> Uni.createFrom().item(invocation.getArgument(0, QAlbum.class)));

        provider.importAlbumById("tdl:123").await().atMost(Duration.ofSeconds(5));

        verify(repository).persist(any(QAlbum.class));
    }

    @Test
    void propagatesPersistenceFailureToSender() {
        when(repository.persist(any(QAlbum.class)))
                .thenReturn(Uni.createFrom().failure(new IllegalStateException("Persistence failed")));

        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> provider.importAlbumById("tdl:123").await().atMost(Duration.ofSeconds(5)));

        assertTrue(hasCause(failure, "Persistence failed"));
    }

    @Test
    void rejectsInvalidPayload() {
        RuntimeException failure = assertThrows(RuntimeException.class,
                () -> imports.send(new AlbumImportMessage("tdl", null)).await().atMost(Duration.ofSeconds(5)));

        assertTrue(hasCause(failure, "Album import message missing album payload"));
    }

    private static boolean hasCause(Throwable failure, String message) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (message.equals(cause.getMessage())) {
                return true;
            }
        }
        return false;
    }
}
