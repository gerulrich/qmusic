package quantum.music.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Represents a music artist containing metadata such as name and picture.
 */
@Schema(name = "Artist", description = "A music artist containing metadata such as name and picture.")
public final class ApiArtist {

    @Schema(description = "Unique identifier for the artist")
    private final String id;
    @Schema(description = "Artist name")
    private final String name;
    @Schema(description = "Short biography or description")
    private final String bio;
    @Schema(description = "URL to the artist picture")
    private final String picture;
    @Schema(description = "Canonical link to the artist resource")
    private final String link;
    @Schema(description = "Link to fetch the artist albums")
    private final String albums;

    private ApiArtist(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.bio = builder.bio;
        this.picture = builder.picture;
        this.link = builder.link;
        this.albums = builder.albums;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ApiArtist template) {
        return new Builder(template);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBio() {
        return bio;
    }

    public String getPicture() {
        return picture;
    }

    public String getLink() {
        return link;
    }

    public String getAlbums() {
        return albums;
    }

    /**
     * Builder for {@link ApiArtist}.
     */
    public static final class Builder {
        private String id;
        private String name;
        private String bio;
        private String picture;
        private String link;
        private String albums;

        private Builder() {
        }

        private Builder(ApiArtist template) {
            this.id = template.id;
            this.name = template.name;
            this.bio = template.bio;
            this.picture = template.picture;
            this.link = template.link;
            this.albums = template.albums;
        }

        /**
         * Sets the identifier.
         */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the name.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the biography snippet.
         */
        public Builder bio(String bio) {
            this.bio = bio;
            return this;
        }

        /**
         * Sets the picture URL.
         */
        public Builder picture(String picture) {
            this.picture = picture;
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
         * Sets the albums link.
         */
        public Builder albums(String albums) {
            this.albums = albums;
            return this;
        }

        /**
         * Creates an immutable {@link ApiArtist}.
         */
        public ApiArtist build() {
            return new ApiArtist(this);
        }
    }
}
