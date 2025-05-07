package quantum.music.service;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.music.client.ApiClient;
import quantum.music.dto.detail.ArtistDetail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static quantum.music.utils.ReflectionUtils.setValue;

@ExtendWith(MockitoExtension.class)
class ArtistServiceTest {

    @Mock
    private ApiClient apiClient;

    @InjectMocks
    private ArtistService artistService;

    @BeforeEach
    void setUp() throws Exception {
       setValue(artistService, "domain", "https://example.com/api");
    }

    @Test
    void testGetArtist() {
        // Setup test data
        JsonObject artistResponse = createArtistResponse();
        when(apiClient.artist(eq("2001"))).thenReturn(Uni.createFrom().item(artistResponse));
        when(apiClient.bio(eq("2001"))).thenReturn(Uni.createFrom().item(new JsonObject()
                .put("text", "Test artist biography")));

        // Execute method
        ArtistDetail artistDetail = artistService.artist("2001")
                .await().indefinitely();

        // Verify results
        assertEquals(2001L, artistDetail.id());
        assertEquals("Test Artist 1", artistDetail.name());
        assertEquals("https://example.com/api/artists/2001/albums", artistDetail.albums());
    }

    // Helper methods to create test JSON responses

    private JsonObject createArtistResponse() {
        return new JsonObject()
                .put("id", 2001)
                .put("name", "Test Artist 1");
    }
}