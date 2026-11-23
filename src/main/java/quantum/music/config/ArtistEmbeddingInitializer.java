package quantum.music.config;

import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import quantum.music.service.ArtistEmbeddingService;

/// Fills missing artist embeddings asynchronously after application startup.
@ApplicationScoped
@UnlessBuildProfile("test")
public class ArtistEmbeddingInitializer {

    private static final Logger LOG = Logger.getLogger(ArtistEmbeddingInitializer.class);

    @Inject
    ArtistEmbeddingService artistEmbeddingService;

    void onStart(@Observes StartupEvent event) {
        artistEmbeddingService.embedMissingArtists().subscribe().with(
            count -> LOG.infof("Generated embeddings for %d artists without an existing embedding", count),
            failure -> LOG.error("Failed to generate embeddings for artists without an existing embedding", failure)
        );
    }
}
