package quantum.music.domain.providers;

/// A stream option for a track, identified by its quality and playback URL.
///
/// @param quality stream quality label
/// @param url URL used to retrieve the stream
public record TrackStream(String quality, String url) {

    /// Creates a builder for a stream option.
    ///
    /// @return a new builder
    public static Builder builder() {
        return new Builder();
    }

    /// Builds a `TrackStream` value.
    public static final class Builder {
        private String quality;
        private String url;

        /// Sets the stream quality label.
        ///
        /// @param quality stream quality
        /// @return this builder
        public Builder quality(String quality) {
            this.quality = quality;
            return this;
        }

        /// Sets the stream URL.
        ///
        /// @param url stream URL
        /// @return this builder
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /// Creates the immutable stream value.
        ///
        /// @return the built stream
        public TrackStream build() {
            return new TrackStream(quality, url);
        }
    }
}
