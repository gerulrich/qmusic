package quantum.music.service;

import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(OpenAiEmbeddingIntegrationTest.OpenAiServer.class)
class OpenAiEmbeddingIntegrationTest {

    @Inject
    ArtistEmbeddingService service;

    @Test
    void sendsAndReadsOpenAiEmbedding() {
        assertEquals(Collections.nCopies(384, 0.25), service.embedQuery("Serafin").await().indefinitely());
        assertEquals("Bearer test-key", OpenAiServer.authorization.get());
        String body = OpenAiServer.body.get();
        assertTrue(body.contains("\"model\":\"text-embedding-3-small\""));
        assertTrue(body.contains("\"input\":\"Serafin\""));
        assertTrue(body.contains("\"dimensions\":384"));
        assertTrue(body.contains("\"encoding_format\":\"float\""));
    }

    public static class OpenAiServer implements QuarkusTestResourceLifecycleManager {
        static final AtomicReference<String> authorization = new AtomicReference<>();
        static final AtomicReference<String> body = new AtomicReference<>();
        private HttpServer server;

        @Override
        public Map<String, String> start() {
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                server.createContext("/v1/embeddings", exchange -> {
                    authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                    body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    String vector = String.join(",", Collections.nCopies(384, "0.25"));
                    byte[] response = ("{\"data\":[{\"index\":0,\"embedding\":[" + vector + "]}]}")
                        .getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                });
                server.start();
                return Map.of(
                    "quarkus.rest-client.openai-embeddings.url", "http://127.0.0.1:" + server.getAddress().getPort(),
                    "openai.api-key", "test-key"
                );
            } catch (IOException e) {
                throw new IllegalStateException("Could not start mock OpenAI server", e);
            }
        }

        @Override
        public void stop() {
            if (server != null) {
                server.stop(0);
            }
        }
    }
}
