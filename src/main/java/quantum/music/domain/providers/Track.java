package quantum.music.domain.providers;

import java.util.List;

/**
 * Provider-agnostic track data returned by any music provider implementation.
 *
 * @param id provider-facing track id
 * @param title track title
 * @param duration track duration in seconds
 * @param trackNumber track index within the album
 * @param volumeNumber disc or volume number within the release
 * @param codec audio codec from the source
 * @param quality audio quality descriptor from the source
 * @param tags provider-specific tags such as format and quality
 * @param version optional version label (e.g., Remaster)
 * @param copyright copyright notice
 */
public record Track(
        String id,
        String title,
        int duration,
        int trackNumber,
        int volumeNumber,
        String codec,
        String quality,
        List<String> tags,
        List<TrackStream> streams,
        String version,
        String copyright) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String title;
        private int duration;
        private int trackNumber;
        private int volumeNumber;
        private String codec;
        private String quality;
        private List<String> tags;
        private List<TrackStream> streams;
        private String version;
        private String copyright;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder duration(int duration) {
            this.duration = duration;
            return this;
        }

        public Builder trackNumber(int trackNumber) {
            this.trackNumber = trackNumber;
            return this;
        }

        public Builder volumeNumber(int volumeNumber) {
            this.volumeNumber = volumeNumber;
            return this;
        }

        public Builder codec(String codec) {
            this.codec = codec;
            return this;
        }

        public Builder quality(String quality) {
            this.quality = quality;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder streams(List<TrackStream> streams) {
            this.streams = streams;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder copyright(String copyright) {
            this.copyright = copyright;
            return this;
        }

        public Track build() {
            return new Track(id, title, duration, trackNumber, volumeNumber, codec, quality, tags, streams, version, copyright);
        }
    }
}
