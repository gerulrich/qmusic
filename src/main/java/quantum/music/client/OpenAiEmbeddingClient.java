package quantum.music.client;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

@Path("/v1/embeddings")
@RegisterRestClient(configKey = "openai-embeddings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface OpenAiEmbeddingClient {

    @POST
    Uni<EmbeddingResponse> embed(@HeaderParam("Authorization") String authorization, EmbeddingRequest request);

    record EmbeddingRequest(String model, String input, int dimensions, String encoding_format) {}

    record EmbeddingResponse(List<EmbeddingData> data) {}

    record EmbeddingData(List<Double> embedding, int index) {}
}
