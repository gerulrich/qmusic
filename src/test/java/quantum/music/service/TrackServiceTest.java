package quantum.music.service;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.music.client.ApiClient;
import quantum.music.dto.detail.MediaInfo;
import quantum.music.dto.detail.TrackDetail;
import quantum.music.dto.summary.Track;
import quantum.music.dto.summary.TrackList;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static quantum.music.utils.ReflectionUtils.setValue;

@ExtendWith(MockitoExtension.class)
class TrackServiceTest {
    @Mock
    private ApiClient apiClient;

    @InjectMocks
    private TrackService trackService;

    @BeforeEach
    void setUp() throws Exception {
        setValue(trackService, "coverUrl", "https://example.com/covers/%s");
        setValue(trackService, "domain", "https://example.com/api");
    }

    @Test
    void testGetTracks() {
        // Setup test data
        JsonObject tracksResponse = createTracksResponse();
        when(apiClient.tracks(eq("1001"))).thenReturn(Uni.createFrom().item(tracksResponse));

        // Execute method
        TrackList trackList = trackService.tracks("1001")
                .await().indefinitely();

        // Verify results
        assertEquals(10, trackList.total());
        assertEquals(2, trackList.tracks().size());
        Track track = trackList.tracks().getFirst();
        assertEquals(3001L, track.id());
        assertEquals("Test Track 1", track.title());
    }

    @Test
    void testGetTrack() {
        // Setup test data
        JsonObject trackResponse = createTrackResponse();
        when(apiClient.track(eq("3001"))).thenReturn(Uni.createFrom().item(trackResponse));

        // Execute method
        TrackDetail trackDetail = trackService.track("3001")
                .await().indefinitely();

        // Verify results
        assertEquals(3001L, trackDetail.id());
        assertEquals("Test Track 1", trackDetail.title());
        assertEquals(1001L, trackDetail.album().id());
        assertEquals("Test Album 1", trackDetail.album().title());
        assertEquals("https://example.com/api/tracks/3001/stream", trackDetail.content());
    }

    @Test
    void testGetContent() {
        // Setup test data
        JsonObject mediaResponse = createMediaResponse();
        when(apiClient.media(eq("3001"), eq("LOSSLESS"), eq("STREAM"), eq("FULL")))
                .thenReturn(Uni.createFrom().item(mediaResponse));

        // Execute method
        MediaInfo mediaInfo = trackService.content("3001")
                .await().indefinitely();

        // Verify results
        assertEquals("https://example.com/stream/test.mp3", mediaInfo.url());
        assertEquals("LOSSLESS", mediaInfo.quality());
        assertEquals("AAC", mediaInfo.codec());
    }

    // Helper methods to create test JSON responses
    private JsonObject createAlbumResponse() {
        JsonObject artist = new JsonObject()
                .put("id", 2001)
                .put("name", "Test Artist 1");

        return new JsonObject()
                .put("id", 1001)
                .put("title", "Test Album 1")
                .put("cover", "123-456-789")
                .put("artist", artist);
    }

    private JsonObject createTracksResponse() {
        JsonObject album = createAlbumResponse();
        JsonObject artist = album.getJsonObject("artist");

        JsonArray items = new JsonArray()
                .add(new JsonObject()
                        .put("id", 3001)
                        .put("title", "Test Track 1")
                        .put("album", album)
                        .put("artist", artist)
                        .put("duration", 240)
                        .put("trackNumber", 1)
                        .put("volumeNumber", 1)
                        .put("audioQuality", "LOSSLESS")
                        .put("mediaMetadata", new JsonObject()
                            .put("tags", new JsonArray().add("tag1").add("tag2"))
                        )
                        .put("version", "1.0")
                        .put("copyright", "Test Copyright"))

                .add(new JsonObject()
                        .put("id", 3002)
                        .put("title", "Test Track 2")
                        .put("album", album)
                        .put("artist", artist)
                        .put("duration", 240)
                        .put("trackNumber", 2)
                        .put("volumeNumber", 1)
                        .put("audioQuality", "LOSSLESS")
                        .put("mediaMetadata", new JsonObject()
                            .put("tags", new JsonArray().add("tag1").add("tag2"))
                        )
                        .put("version", "1.0")
                        .put("copyright", "Test Copyright"));

        return new JsonObject()
                .put("items", items)
                .put("totalNumberOfItems", 10);
    }

    private JsonObject createTrackResponse() {
        JsonObject album = createAlbumResponse();

        return new JsonObject()
                .put("id", 3001)
                .put("title", "Test Track 1")
                .put("album", album)
                .put("duration", 240)
                .put("trackNumber", 1)
                .put("volumeNumber", 1)
                .put("audioQuality", "LOSSLESS")
                .put("mediaMetadata", new JsonObject()
                        .put("tags", new JsonArray().add("tag1").add("tag2"))
                )
                .put("version", "1.0")
                .put("copyright", "Test Copyright");
    }

    private JsonObject createMediaResponse() {
        // Create encoded manifest
        JsonObject manifest = new JsonObject()
                .put("urls", new JsonArray().add("https://example.com/stream/test.mp3"))
                .put("codecs", "AAC");

        String encodedManifest = Base64.getEncoder().encodeToString(manifest.toString().getBytes());

        return new JsonObject()
                .put("manifest", encodedManifest)
                .put("audioQuality", "LOSSLESS");
    }
}