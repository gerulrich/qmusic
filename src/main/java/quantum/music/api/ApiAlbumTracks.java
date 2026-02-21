package quantum.music.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * Represents a collection of album tracks containing metadata such as album and tracks.
 */
@Schema(name = "AlbumTracks", description = "A collection of album tracks containing metadata such as album and tracks.")
public final class ApiAlbumTracks {

    @Schema(description = "Total number of tracks in the album")
    private final int total;
    @Schema(description = "List of tracks in the album", implementation = ApiTrack.class)
    private final List<ApiTrack> tracks;

    private ApiAlbumTracks(Builder builder) {
        this.total = builder.total;
        this.tracks = builder.tracks != null ? List.copyOf(builder.tracks) : null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ApiAlbumTracks template) {
        return new Builder(template);
    }

    public int getTotal() {
        return total;
    }

    public List<ApiTrack> getTracks() {
        return tracks;
    }

    /**
     * Builder for {@link ApiAlbumTracks}.
     */
    public static final class Builder {
        private int total;
        private List<ApiTrack> tracks;

        private Builder() {
        }

        private Builder(ApiAlbumTracks template) {
            this.total = template.total;
            this.tracks = template.tracks;
        }

        /**
         * Sets the total number of tracks.
         */
        public Builder total(int total) {
            this.total = total;
            return this;
        }

        /**
         * Sets the list of track DTOs.
         */
        public Builder tracks(List<ApiTrack> tracks) {
            this.tracks = tracks;
            return this;
        }

        /**
         * Creates an immutable {@link ApiAlbumTracks} instance.
         */
        public ApiAlbumTracks build() {
            return new ApiAlbumTracks(this);
        }
    }
}
