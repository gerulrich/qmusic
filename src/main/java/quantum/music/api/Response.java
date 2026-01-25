package quantum.music.api;

import java.util.List;

/**
 * Factory helpers for API response DTOs.
 */
public final class Response {

    private Response() {
    }

    /**
     * Fluent factory for {@link ListResponse} instances.
     *
     * @param items payload items
     * @return builder limited to {@link ListResponse}
     */
    public static <T> ListResponse.Builder<T> list(List<T> items) {
        return ListResponse.list(items);
    }
}
