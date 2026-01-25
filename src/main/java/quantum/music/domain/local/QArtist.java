package quantum.music.domain.local;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

/**
 * Artist entity stored in the local MongoDB collection.
 */
@MongoEntity(collection="artists")
public class QArtist {

    /**
     * Unique identifier for the artist document in MongoDB.
     */
    public ObjectId id;
    /**
     * Display name of the artist.
     */
    public String name;

    /**
     * Short biography or description text.
     */
    public String bio;

    /**
     * Internal document version field.
     */
    @BsonProperty("__v")
    public int version;
}
