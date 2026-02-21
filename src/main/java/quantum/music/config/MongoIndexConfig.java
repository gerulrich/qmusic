package quantum.music.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.bson.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Configuration class to create MongoDB indexes at application startup.
 */
@ApplicationScoped
@UnlessBuildProfile("test")
public class MongoIndexConfig {

    private static final Logger LOG = Logger.getLogger(MongoIndexConfig.class);

    @Inject
    MongoClient mongoClient;

    @ConfigProperty(name = "quarkus.mongodb.database")
    String database;

    /**
     * Creates text indexes for Album collection on application startup.
     * This allows full-text search on title, artist, and albumArtist fields.
     *
     * @param event the startup event
     */
    void onStart(@Observes StartupEvent event) {
        LOG.info("Creating MongoDB text indexes...");
        createAlbumTextIndex();
        LOG.info("MongoDB indexes created successfully");
    }

    private void createAlbumTextIndex() {
        try {
            MongoCollection<Document> albumsCollection = mongoClient
                    .getDatabase(database)
                    .getCollection("albums");

            // Create a compound text index on multiple fields
            // This allows searching across title, artist, and album_artist fields
            albumsCollection.createIndex(
                    Indexes.compoundIndex(
                            Indexes.text("title"),
                            Indexes.text("artist"),
                            Indexes.text("album_artist")
                    ),
                    new IndexOptions().name("album_text_index")
            );

            LOG.info("Text index created for albums collection");
        } catch (Exception e) {
            LOG.error("Error creating text index for albums", e);
        }
    }
}
