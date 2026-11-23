package quantum.music.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenq.BgeSmallEnQuantizedEmbeddingModel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/// Provides the local BGE embedding model to application services.
@ApplicationScoped
public class EmbeddingModelProducer {

    @Produces
    @ApplicationScoped
    EmbeddingModel embeddingModel() {
        return new BgeSmallEnQuantizedEmbeddingModel();
    }
}
