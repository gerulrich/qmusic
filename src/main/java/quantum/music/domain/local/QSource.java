package quantum.music.domain.local;

/**
 * Source metadata describing where and how the album was obtained.
 */
public class QSource {
    /**
     * Provider-specific source identifier.
     */
    public String id;
    /**
     * Provider display name.
     */
    public String name;
    /**
     * Source type (provider category or origin).
     */
    public String type;
    /**
     * Media format (e.g., FLAC, MP3).
     */
    public String format;
    /**
     * Quality descriptor (e.g., lossless, 320kbps).
     */
    public String quality;
    /**
     * Source status (e.g., available, pending).
     */
    public String status;
}
