package quantum.music.domain.local;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

/**
 * Track entity embedded within an album document.
 */
public class QTrack {
    /**
     * Track title as displayed to users.
     */
    public String title;
    /**
     * Track artist display name.
     */
    public String artist;
    /**
     * Track position within the disc (1-based).
     */
    @BsonProperty("track_number")
    public int trackNumber;
    /**
     * Disc position within a multi-disc release (1-based).
     */
    @BsonProperty("disc_number")
    public int discNumber;
    /**
     * Free-form comments or notes associated with the track.
     */
    public String comments;
    /**
     * International Standard Recording Code, when available.
     */
    public String isrc;
    /**
     * Track duration in seconds.
     */
    public int duration;

    /**
     * Unique identifier for the embedded track document.
     */
    public ObjectId _id;
    /**
     * Local filesystem path to the media file, when available.
     */
    @BsonProperty("file_path")
    public String filePath;
}
