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
import quantum.music.dto.detail.AlbumDetail;
import quantum.music.dto.summary.Album;
import quantum.music.dto.summary.ItemsResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static quantum.music.utils.ReflectionUtils.setValue;

@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock
    private ApiClient apiClient;

    @InjectMocks
    private AlbumService albumService;

    @BeforeEach
    void setUp() throws Exception {
        setValue(albumService, "coverUrl", "https://example.com/covers/%s");
        setValue(albumService, "domain", "https://example.com/api");
    }

    @Test
    void testSearchAlbums() {
        // Setup test data
        JsonObject searchResponse = createSearchResponse();
        when(apiClient.search(eq("test query"), eq("ALBUMS"), eq(0), eq(10)))
                .thenReturn(Uni.createFrom().item(searchResponse));

        // Execute method
        ItemsResponse<Album> response = albumService.search("test query", 0, 10)
                .await().indefinitely();

        // Verify results
        assertEquals(2, response.items().size());
        assertEquals(20, response.total());
        assertEquals(0, response.offset());
        assertEquals(10, response.limit());

        Album album = response.items().getFirst();
        assertEquals(1001L, album.id());
        assertEquals("Test Album 1", album.title());
        assertEquals(2001L, album.artist().id());
        assertEquals("Test Artist 1", album.artist().name());
        assertEquals("https://example.com/covers/123/456/789", album.cover());
        assertEquals("https://example.com/api/albums/1001", album.link());
    }

    @Test
    void testSearchAlbumsWithError() {
        // Setup test data with error
        when(apiClient.search(eq("test query"), eq("ALBUMS"), eq(0), eq(10)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("API error")));

        // Execute and verify exception
        Exception exception = assertThrows(RuntimeException.class, () -> {
            albumService.search("test query", 0, 10)
                    .await().indefinitely();
        });

        assertEquals("API error", exception.getMessage());
    }

    @Test
    void testGetAlbums() {
        // Setup test data
        JsonObject albumsResponse = createArtistAlbumsResponse();
        when(apiClient.albums(eq("2001"), eq(0), eq(10)))
                .thenReturn(Uni.createFrom().item(albumsResponse));

        // Execute method
        ItemsResponse<Album> response = albumService.albums("2001", 0, 10)
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
        AlbumDetail albumDetail = albumService.album("1001")
                .await().indefinitely();

        // Verify results
        assertEquals(1001L, albumDetail.id());
        assertEquals("Test Album 1", albumDetail.title());
        assertEquals(2001L, albumDetail.artist().id());
        assertEquals("Test Artist 1", albumDetail.artist().name());
        assertEquals("https://example.com/covers/123/456/789", albumDetail.cover());
        assertEquals("https://example.com/api/albums/1001/tracks", albumDetail.tracks());
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
}