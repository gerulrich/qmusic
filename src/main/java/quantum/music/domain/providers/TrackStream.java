package quantum.music.domain.providers;

public record TrackStream(String quality, String url) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String quality;
        private String url;

        public Builder quality(String quality) {
            this.quality = quality;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public TrackStream build() {
            return new TrackStream(quality, url);
        }
    }
}
