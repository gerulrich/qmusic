package quantum.music.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Represents a music track stream containing metadata such as codec, quality, and stream URL.
 */
@Schema(name = "TrackStream", description = "A music track stream containing metadata such as codec, quality, and stream URL.")
public final class ApiTrackStream {

    @Schema(description = "Audio codec used in the stream (e.g. flac, aac)")
    private final String codec;
    @Schema(description = "Quality or bitrate designation (e.g. lossless, 320kbps)")
    private final String quality;
    @Schema(description = "URL to request the stream")
    private final String url;

    private ApiTrackStream(Builder builder) {
        this.codec = builder.codec;
        this.quality = builder.quality;
        this.url = builder.url;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ApiTrackStream template) {
        return new Builder(template);
    }

    public String getCodec() {
        return codec;
    }

    public String getQuality() {
        return quality;
    }

    public String getUrl() {
        return url;
    }

    public static final class Builder {
        private String codec;
        private String quality;
        private String url;

        private Builder() {
        }

        private Builder(ApiTrackStream template) {
            this.codec = template.codec;
            this.quality = template.quality;
            this.url = template.url;
        }

        /**
         * Sets the audio codec for the track stream.
         *
         * @param codec the codec to set
         * @return the builder instance
         */
        public Builder codec(String codec) {
            this.codec = codec;
            return this;
        }

        /**
         * Sets the quality or bitrate for the track stream.
         *
         * @param quality the quality to set
         * @return the builder instance
         */
        public Builder quality(String quality) {
            this.quality = quality;
            return this;
        }

        /**
         * Sets the URL for the track stream.
         *
         * @param url the URL to set
         * @return the builder instance
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        /**
         * Builds the ApiTrackStream instance.
         *
         * @return the ApiTrackStream instance
         */
        public ApiTrackStream build() {
            return new ApiTrackStream(this);
        }
    }
}
