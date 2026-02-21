package quantum.music.domain.providers;

import java.util.List;

/**
 * Provider-agnostic album data returned by any music provider implementation.
 *
 * @param id provider-facing album id
 * @param title album title
 * @param artist primary artist metadata
 * @param release release date or year
 * @param copyright copyright notice
 * @param type album type (e.g., ALBUM)
 * @param cover cover image URL
 * @param tags provider-specific tags such as format and quality
 */
public record Album(
        String id,
        String title,
        Artist artist,
        String release,
        String copyright,
        String type,
        String cover,
        List<String> tags) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String title;
        private Artist artist;
        private String release;
        private String copyright;
        private String type;
        private String cover;
        private List<String> tags;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder artist(Artist artist) {
            this.artist = artist;
            return this;
        }

        public Builder release(String release) {
            this.release = release;
            return this;
        }

        public Builder copyright(String copyright) {
            this.copyright = copyright;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder cover(String cover) {
            this.cover = cover;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Album build() {
            return new Album(id, title, artist, release, copyright, type, cover, tags);
        }
    }
}