package quantum.music.api;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Represents a music track containing metadata such as title, artist, and duration.
 */
@Schema(name = "Track", description = "A music track containing metadata such as title, artist, and duration.")
public final class ApiTrack {

    @Schema(description = "Unique identifier for the track")
    private final String id;
    @Schema(description = "Track title")
    private final String title;
    @Schema(description = "Track duration in seconds")
    private final int duration;
    @Schema(description = "Artist of the track", implementation = ApiArtist.class)
    private final ApiArtist artist;
    @Schema(description = "Position within the disc or digital listing")
    private final int track;
    @Schema(description = "Disc/volume number for multi-disc releases")
    private final int volume;
    @Schema(description = "Album that owns the track", implementation = ApiAlbum.class)
    private final ApiAlbum album;
    @Schema(description = "Audio codec of the best available stream")
    private final String codec;
    @Schema(description = "Tags describing quality, mood or mix variants")
    private final List<String> tags;
    @Schema(description = "Alternative mix or master identifier")
    private final String version;
    @Schema(description = "Copyright notice for the track")
    private final String copyright;
    @Schema(description = "Canonical link to the track resource")
    private final String link;
    @Schema(description = "Available streaming endpoints", implementation = ApiTrackStream.class)
    private final List<ApiTrackStream> streams;

    private ApiTrack(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.duration = builder.duration;
        this.track = builder.track;
        this.volume = builder.volume;
        this.album = builder.album;
        this.artist = builder.artist;
        this.codec = builder.codec;
        this.tags = builder.tags != null ? List.copyOf(builder.tags) : null;
        this.version = builder.version;
        this.copyright = builder.copyright;
        this.link = builder.link;
        this.streams = builder.streams != null ? List.copyOf(builder.streams) : null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ApiTrack template) {
        return new Builder(template);
    }

    public String id() {
        return id;
    }

    /**
     * Gets the unique identifier for the track.
     *
     * @return the track ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the title of the track.
     *
     * @return the track title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the duration of the track in seconds.
     *
     * @return the track duration
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Gets the track number within the volume.
     *
     * @return the track number
     */
    public int getTrack() {
        return track;
    }

    /**
     * Gets the disc or volume number for multi-disc releases.
     *
     * @return the volume number
     */
    public int getVolume() {
        return volume;
    }

    /**
     * Gets the album that owns the track.
     *
     * @return the album
     */
    public ApiAlbum getAlbum() {
        return album;
    }

    /**
     * Gets the artist of the track.
     *
     * @return the artist
     */
    public ApiArtist getArtist() {
        return artist;
    }

    /**
     * Gets the audio codec of the best available stream.
     *
     * @return the codec
     */
    public String getCodec() {
        return codec;
    }

    /**
     * Gets the tags describing the track variant.
     *
     * @return the list of tags
     */
    public List<String> getTags() {
        return tags;
    }

    /**
     * Gets the version string for the track.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Gets the copyright notice for the track.
     *
     * @return the copyright notice
     */
    public String getCopyright() {
        return copyright;
    }

    /**
     * Gets the canonical link to the track resource.
     *
     * @return the canonical link
     */
    public String getLink() {
        return link;
    }

    /**
     * Gets the available streaming endpoints for the track.
     *
     * @return the list of streaming endpoints
     */
    public List<ApiTrackStream> getStreams() {
        return streams;
    }

    /**
     * Builder for {@link ApiTrack}.
     */
    public static final class Builder {
        private String id;
        private String title;
        private int duration;
        private int track;
        private int volume;
        private ApiAlbum album;
        private ApiArtist artist;
        private String codec;
        private List<String> tags;
        private String version;
        private String copyright;
        private String link;
        private List<ApiTrackStream> streams;

        private Builder() {
        }

        private Builder(ApiTrack template) {
            this.id = template.id;
            this.title = template.title;
            this.duration = template.duration;
            this.track = template.track;
            this.volume = template.volume;
            this.album = template.album;
            this.artist = template.artist;
            this.codec = template.codec;
            this.tags = template.tags;
            this.version = template.version;
            this.copyright = template.copyright;
            this.link = template.link;
            this.streams = template.streams;
        }

        /**
         * Sets the identifier.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the title.
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the duration in seconds.
         */
        public Builder duration(int duration) {
            this.duration = duration;
            return this;
        }

        /**
         * Sets the track number within the volume.
         */
        public Builder track(int track) {
            this.track = track;
            return this;
        }

        /**
         * Sets the disc or volume number.
         */
        public Builder volume(int volume) {
            this.volume = volume;
            return this;
        }

        /**
         * Assigns the album DTO.
         */
        public Builder album(ApiAlbum album) {
            this.album = album;
            return this;
        }

        /**
         * Assigns the artist DTO.
         */
        public Builder artist(ApiArtist artist) {
            this.artist = artist;
            return this;
        }

        /**
         * Sets the codec identifier.
         */
        public Builder codec(String codec) {
            this.codec = codec;
            return this;
        }

        /**
         * Sets the tags describing the track variant.
         */
        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Sets the version string.
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the copyright notice.
         */
        public Builder copyright(String copyright) {
            this.copyright = copyright;
            return this;
        }

        /**
         * Sets the canonical link.
         */
        public Builder link(String link) {
            this.link = link;
            return this;
        }

        /**
         * Sets the available stream descriptors.
         */
        public Builder streams(List<ApiTrackStream> streams) {
            this.streams = streams;
            return this;
        }

        /**
         * Creates an immutable {@link ApiTrack}.
         */
        public ApiTrack build() {
            return new ApiTrack(this);
        }
    }
}