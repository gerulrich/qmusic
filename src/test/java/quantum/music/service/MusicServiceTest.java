package quantum.music.service;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.music.client.ApiClient;
import quantum.music.dto.detail.AlbumDetail;
import quantum.music.dto.detail.ArtistDetail;
import quantum.music.dto.detail.MediaInfo;
import quantum.music.dto.detail.TrackDetail;
import quantum.music.dto.summary.*;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MusicServiceTest {

    @Mock
    @RestClient
    private ApiClient apiClient;

    @InjectMocks
    private MusicService musicService;

    private final String testCoverUrl = "https://example.com/covers/%s";
    private final String testDomain = "https://example.com/api";

    @BeforeEach
    void setUp() throws Exception {
        // Set values for config properties using reflection
        java.lang.reflect.Field coverUrlField = MusicService.class.getDeclaredField("coverUrl");
        coverUrlField.setAccessible(true);
        coverUrlField.set(musicService, testCoverUrl);
        
        java.lang.reflect.Field domainField = MusicService.class.getDeclaredField("domain");
        domainField.setAccessible(true);
        domainField.set(musicService, testDomain);
    }

    //@Test
    void testSearchAlbums() {
        // Setup test data
        JsonObject searchResponse = createSearchResponse();
        when(apiClient.search(eq("test query"), eq("ALBUMS"), eq(0), eq(10)))
                .thenReturn(Uni.createFrom().item(searchResponse));

        // Execute method
        ItemsResponse<Album> response = musicService.search("test query", 0, 10)
                .await().indefinitely();

        // Verify results
        assertEquals(2, response.items().size());
        assertEquals(20, response.total());
        assertEquals(0, response.offset());
        assertEquals(10, response.limit());

        Album album = response.items().get(0);
        assertEquals(1001L, album.id());
        assertEquals("Test Album 1", album.title());
        assertEquals(2001L, album.artist().id());
        assertEquals("Test Artist 1", album.artist().name());
        assertEquals("https://example.com/covers/123/456/789", album.cover());
        assertEquals("https://example.com/api/albums/1001", album.cover());
    }

    @Test
    void testSearchAlbumsWithError() {
        // Setup test data with error
        when(apiClient.search(eq("test query"), eq("ALBUMS"), eq(0), eq(10)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("API error")));

        // Execute and verify exception
        Exception exception = assertThrows(RuntimeException.class, () -> {
            musicService.search("test query", 0, 10)
                    .await().indefinitely();
        });
        
        assertEquals("API error", exception.getMessage());
    }

    @Test
    void testGetArtist() {
        // Setup test data
        JsonObject artistResponse = createArtistResponse();
        when(apiClient.artist(eq("2001"))).thenReturn(Uni.createFrom().item(artistResponse));

        // Execute method
        ArtistDetail artistDetail = musicService.artist("2001")
                .await().indefinitely();

        // Verify results
        assertEquals(2001L, artistDetail.id());
        assertEquals("Test Artist 1", artistDetail.name());
        assertEquals("https://example.com/api/artists/2001/albums", artistDetail.albums());
    }

    //@Test
    void testGetAlbums() {
        // Setup test data
        JsonObject albumsResponse = createArtistAlbumsResponse();
        when(apiClient.albums(eq("2001"), eq(0), eq(10)))
                .thenReturn(Uni.createFrom().item(albumsResponse));

        // Execute method
        ItemsResponse<Album> response = musicService.albums("2001", 0, 10)
                .await().indefinitely();

        // Verify results
        assertEquals(2, response.items().size());
        assertEquals(10, response.total());
        assertEquals(0, response.offset());
        assertEquals(10, response.limit());
    }

    @Test
    void testGetAlbum() {
        // Setup test data
        JsonObject albumResponse = createAlbumResponse();
        when(apiClient.album(eq("1001"))).thenReturn(Uni.createFrom().item(albumResponse));

        // Execute method
        AlbumDetail albumDetail = musicService.album("1001")
                .await().indefinitely();

        // Verify results
        assertEquals(1001L, albumDetail.id());
        assertEquals("Test Album 1", albumDetail.title());
        assertEquals(2001L, albumDetail.artist().id());
        assertEquals("Test Artist 1", albumDetail.artist().name());
        assertEquals("https://example.com/covers/123/456/789", albumDetail.cover());
        assertEquals("https://example.com/api/albums/1001/tracks", albumDetail.tracks());
    }

    @Test
    void testGetTracks() {
        // Setup test data
        JsonObject tracksResponse = createTracksResponse();
        when(apiClient.tracks(eq("1001"))).thenReturn(Uni.createFrom().item(tracksResponse));

        // Execute method
        TrackList trackList = musicService.tracks("1001")
                .await().indefinitely();

        // Verify results
        assertEquals(10, trackList.total());
        assertEquals(2, trackList.tracks().size());
        Track track = trackList.tracks().get(0);
        assertEquals(3001L, track.id());
        assertEquals("Test Track 1", track.title());
    }

    @Test
    void testGetTrack() {
        // Setup test data
        JsonObject trackResponse = createTrackResponse();
        when(apiClient.track(eq("3001"))).thenReturn(Uni.createFrom().item(trackResponse));

        // Execute method
        TrackDetail trackDetail = musicService.track("3001")
                .await().indefinitely();

        // Verify results
        assertEquals(3001L, trackDetail.id());
        assertEquals("Test Track 1", trackDetail.title());
        assertEquals(1001L, trackDetail.album().id());
        assertEquals("Test Album 1", trackDetail.album().title());
        assertEquals("https://example.com/api/tracks/3001/content", trackDetail.content());
    }

    @Test
    void testGetContent() {
        // Setup test data
        JsonObject mediaResponse = createMediaResponse();
        when(apiClient.media(eq("3001"), eq("LOSSLESS"), eq("STREAM"), eq("FULL")))
                .thenReturn(Uni.createFrom().item(mediaResponse));

        // Execute method
        MediaInfo mediaInfo = musicService.content("3001")
                .await().indefinitely();

        // Verify results
        assertEquals("https://example.com/stream/test.mp3", mediaInfo.url());
        assertEquals("LOSSLESS", mediaInfo.quality());
        assertEquals("AAC", mediaInfo.codec());
    }

    // Helper methods to create test JSON responses

    private JsonObject createSearchResponse() {
        JsonArray artists = new JsonArray()
                .add(new JsonObject()
                        .put("id", 2001)
                        .put("name", "Test Artist 1"));

        JsonArray items = new JsonArray()
                .add(new JsonObject()
                        .put("id", 1001)
                        .put("title", "Test Album 1")
                        .put("cover", "123-456-789")
                        .put("artists", artists))
                .add(new JsonObject()
                        .put("id", 1002)
                        .put("title", "Test Album 2")
                        .put("cover", "123-456-790")
                        .put("artists", artists));

        JsonObject albumsObj = new JsonObject()
                .put("items", items)
                .put("totalNumberOfItems", 20);

        return new JsonObject().put("albums", albumsObj);
    }

    private JsonObject createArtistResponse() {
        return new JsonObject()
                .put("id", 2001)
                .put("name", "Test Artist 1");
    }

    private JsonObject createArtistAlbumsResponse() {
        JsonObject artist = new JsonObject()
                .put("id", 2001)
                .put("name", "Test Artist 1");

        JsonArray items = new JsonArray()
                .add(new JsonObject()
                        .put("id", 1001)
                        .put("title", "Test Album 1")
                        .put("cover", "123-456-789")
                        .put("artist", artist))
                .add(new JsonObject()
                        .put("id", 1002)
                        .put("title", "Test Album 2")
                        .put("cover", "123-456-790")
                        .put("artist", artist));

        return new JsonObject()
                .put("items", items)
                .put("offset", 0)
                .put("limit", 10)
                .put("totalNumberOfItems", 10);
    }

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
                        .put("artist", artist))
                .add(new JsonObject()
                        .put("id", 3002)
                        .put("title", "Test Track 2")
                        .put("album", album)
                        .put("artist", artist));

        return new JsonObject()
                .put("items", items)
                .put("totalNumberOfItems", 10);
    }

    private JsonObject createTrackResponse() {
        JsonObject album = createAlbumResponse();
        
        return new JsonObject()
                .put("id", 3001)
                .put("title", "Test Track 1")
                .put("album", album);
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
