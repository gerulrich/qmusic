package quantum.music.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * Represents a music album containing metadata such as title, artist, and cover art.
 */
@Schema(name = "Album", description = "A music album containing metadata such as title, artist, and cover art.")
public final class ApiAlbum {

    @Schema(description = "Unique identifier for the album")
    private final String id;
    @Schema(description = "Album title")
    private final String title;
    @Schema(description = "Release date (ISO-8601)")
    private final String release;
    @Schema(description = "Artist of the album", implementation = ApiArtist.class)
    private final ApiArtist artist;
    @Schema(description = "URL to the album cover art")
    private final String cover;
    @Schema(description = "Copyright notice or rights holder information")
    private final String copyright;
    @Schema(description = "Genre, mood or custom tags associated with the album")
    private final List<String> tags;
    @Schema(description = "Album type (e.g. LP, EP, single)")
    private final String type;
    @Schema(description = "Canonical link to the album resource")
    private final String link;
    @Schema(description = "Link to fetch the tracks that belong to this album")
    private final String tracks;

    private ApiAlbum(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.release = builder.release;
        this.artist = builder.artist;
        this.cover = builder.cover;
        this.copyright = builder.copyright;
        this.tags = builder.tags != null ? List.copyOf(builder.tags) : null;
        this.type = builder.type;
        this.link = builder.link;
        this.tracks = builder.tracks;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ApiAlbum template) {
        return new Builder(template);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getRelease() {
        return release;
    }

    public ApiArtist getArtist() {
        return artist;
    }

    public String getCover() {
        return cover;
    }

    public String getCopyright() {
        return copyright;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getType() {
        return type;
    }

    public String getLink() {
        return link;
    }

    public String getTracks() {
        return tracks;
    }

    /**
     * Builder for {@link ApiAlbum} that performs defensive copying when creating the DTO.
     */
    public static final class Builder {
        private String id;
        private String title;
        private String release;
        private ApiArtist artist;
        private String cover;
        private String copyright;
        private List<String> tags;
        private String type;
        private String link;
        private String tracks;

        private Builder() {
        }

        private Builder(ApiAlbum template) {
            this.id = template.id;
            this.title = template.title;
            this.release = template.release;
            this.artist = template.artist;
            this.cover = template.cover;
            this.copyright = template.copyright;
            this.tags = template.tags;
            this.type = template.type;
            this.link = template.link;
            this.tracks = template.tracks;
        }

        /**
         * Sets the catalog identifier.
         * @param id stable identifier used to reference the album
         * @return this builder for chaining
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the public-facing album title.
         * @param title human-readable album title
         * @return this builder for chaining
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets the release date.
         * @param release release date expressed as ISO-8601 string
         * @return this builder for chaining
         */
        public Builder release(String release) {
            this.release = release;
            return this;
        }

        /**
         * Assigns the artist DTO.
         * @param artist artist representation that owns the album
         * @return this builder for chaining
         */
        public Builder artist(ApiArtist artist) {
            this.artist = artist;
            return this;
        }

        /**
         * Sets the cover URL.
         * @param cover absolute link to the album artwork
         * @return this builder for chaining
         */
        public Builder cover(String cover) {
            this.cover = cover;
            return this;
        }

        /**
         * Sets the copyright notice.
         * @param copyright copyright or rights holder text
         * @return this builder for chaining
         */
        public Builder copyright(String copyright) {
            this.copyright = copyright;
            return this;
        }

        /**
         * Sets the tags list.
         * @param tags descriptive tags applied to the album
         * @return this builder for chaining
         */
        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Sets the album type.
         * @param type album classification such as LP, EP or single
         * @return this builder for chaining
         */
        public Builder type(String type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the canonical link.
         * @param link absolute link to the album resource
         * @return this builder for chaining
         */
        public Builder link(String link) {
            this.link = link;
            return this;
        }

        /**
         * Sets the tracks link.
         * @param tracks endpoint to retrieve the album tracks
         * @return this builder for chaining
         */
        public Builder tracks(String tracks) {
            this.tracks = tracks;
            return this;
        }

        /**
         * Creates an immutable {@link ApiAlbum} instance.
         * @return the constructed DTO
         */
        public ApiAlbum build() {
            return new ApiAlbum(this);
        }
    }
}
