package quantum.music.domain;

import java.util.List;

/**
 * Represents a generic paginated response for domain entities in the qmusic app.
 * <p>
 * This record is designed to encapsulate a page of domain objects—such as albums, artists, or tracks—
 * along with pagination metadata. It is primarily used in the domain layer to efficiently
 * deliver large collections of musical resources in a structured, paginated format. This approach
 * supports scalable data retrieval and consistent API responses throughout the qmusic app.
 *
 * @param <T>   the type of the domain entity contained in the response (e.g., Album, Artist, Track)
 * @param items the list of domain entities for the current page
 * @param offset the zero-based index indicating the start of the current page in the full collection
 * @param limit  the maximum number of entities returned per page
 * @param total  the total number of entities available in the full collection
 */
public record PagedResponse<T>(
    List<T> items,
    int offset,
    int limit,
    int total) {
}
