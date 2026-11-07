package quantum.music.resource;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import quantum.music.domain.providers.Track;
import quantum.music.domain.providers.TrackDetail;
import quantum.music.providers.MusicProvider;
import quantum.music.service.ProviderService;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.*;

@QuarkusTest
class TrackResourceTest {

    @InjectMock
    ProviderService providerService;

    @Test
    @TestSecurity(user = "test-user")
    void testDownloadEndpoint() {
        MusicProvider mockProvider = Mockito.mock(MusicProvider.class);
        Track mockTrack = Track.builder()
                .title("Test Track")
                .codec("flac")
                .build();
        TrackDetail mockDetail = new TrackDetail(null, mockTrack);

        Mockito.when(providerService.fromId("tdl:123")).thenReturn(Uni.createFrom().item(mockProvider));
        Mockito.when(mockProvider.getTrackById("tdl:123")).thenReturn(Uni.createFrom().item(mockDetail));
        Mockito.when(mockProvider.streamTrackById(eq("tdl:123"), any(), any(), any()))
                .thenReturn(Multi.createFrom().item(Buffer.buffer("audio content")));

        given()
            .when().get("/music/tracks/tdl:123/download")
            .then()
                .statusCode(200)
                .header("Content-Disposition", "attachment; filename=\"Test Track.flac\"")
                .body(is("audio content"));
    }

    @Test
    @TestSecurity(user = "test-user")
    void testStreamEndpoint() {
        MusicProvider mockProvider = Mockito.mock(MusicProvider.class);
        Track mockTrack = Track.builder()
                .title("Test Track")
                .codec("flac")
                .build();
        TrackDetail mockDetail = new TrackDetail(null, mockTrack);

        Mockito.when(providerService.fromId("tdl:123")).thenReturn(Uni.createFrom().item(mockProvider));
        Mockito.when(mockProvider.getTrackById("tdl:123")).thenReturn(Uni.createFrom().item(mockDetail));
        Mockito.when(mockProvider.streamTrackById(eq("tdl:123"), any(), any(), any()))
                .thenReturn(Multi.createFrom().item(Buffer.buffer("audio content")));

        given()
            .when().get("/music/tracks/tdl:123/stream")
            .then()
                .statusCode(200)
                .contentType("audio/flac")
                .body(is("audio content"));
    }

    @Test
    @TestSecurity(user = "test-user")
    void testDownloadEndpointNotFound() {
        MusicProvider mockProvider = Mockito.mock(MusicProvider.class);

        Mockito.when(providerService.fromId("tdl:404")).thenReturn(Uni.createFrom().item(mockProvider));
        Mockito.when(mockProvider.getTrackById("tdl:404")).thenReturn(Uni.createFrom().nullItem());

        given()
            .when().get("/music/tracks/tdl:404/download")
            .then()
                .statusCode(404);
    }
}
