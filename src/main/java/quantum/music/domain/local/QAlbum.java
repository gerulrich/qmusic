package quantum.music.domain.local;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;
import java.util.List;

/**
 * Album entity stored in the local MongoDB collection.
 * Fields mirror the persisted document, including embedded tracks and source.
 */
@MongoEntity(collection="albums")
public class QAlbum {

    /**
     * Unique identifier for the album document in MongoDB.
     */
    public ObjectId id;

    /**
     * Album title as displayed to users.
     */
    public String title;

    /**
     * Primary artist display name for the album.
     */
    public String artist;

    /**
     * Album artist display name when different from track artist.
     */
    @BsonProperty("album_artist")
    public String albumArtist;

    /**
     * Free-form comments or notes associated with the album.
     */
    public String comments;

    /**
     * Universal Product Code for the release, when available.
     */
    public String upc;

    /**
     * Reference to the artist document id.
     */
    @BsonProperty("artist_id")
    public ObjectId artistId;

    /**
     * Cover image reference (URL or identifier).
     */
    public String cover;

    /**
     * Release information (date/year string or label text).
     */
    public String release;

    /**
     * Copyright or rights statement.
     */
    public String copyright;

    /**
     * Source metadata for the album (provider, format, quality).
     */
    public QSource source;

    /**
     * Tracks included in the album.
     */
    public List<QTrack> tracks;

    /**
     * Internal document version field.
     */
    @BsonProperty("__v")
    public int version;
}
