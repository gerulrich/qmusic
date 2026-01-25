package quantum.music.api;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * List response representation.
 */
@Schema(name = "ListResponse", description = "List response with pagination metadata")
public record ListResponse<T>(
        List<T> items,
        @Schema int offset,
        @Schema int limit,
        @Schema int total
) {

    public static <T> Builder<T> list(List<T> items) {
        return new Builder<>(items);
    }

    public static final class Builder<T> {
        private List<T> items;
        private int offset;
        private int limit;
        private int total;

        private Builder(List<T> items) {
            this.items = items;
        }

        public Builder<T> items(List<T> items) {
            this.items = items;
            return this;
        }

        public Builder<T> offset(int offset) {
            this.offset = offset;
            return this;
        }

        public Builder<T> limit(int limit) {
            this.limit = limit;
            return this;
        }

        public Builder<T> total(int total) {
            this.total = total;
            return this;
        }

        public ListResponse<T> build() {
            return new ListResponse<>(items, offset, limit, total);
        }
    }
}

