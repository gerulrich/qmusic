package quantum.music.domain.providers;

/**
 * Provider-agnostic artist data returned by any music provider implementation.
 *
 * @param id provider-facing artist id
 * @param name artist name
 * @param bio optional artist biography
 */
public record Artist(String id, String name, String bio, String picture) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String name;
        private String bio;
        private String picture;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder bio(String bio) {
            this.bio = bio;
            return this;
        }

        public Builder picture(String picture) {
            this.picture = picture;
            return this;
        }

        public Artist build() {
            return new Artist(id, name, bio, picture);
        }
    }
}
