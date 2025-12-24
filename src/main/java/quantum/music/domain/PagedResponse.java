package quantum.music.domain;

import java.util.List;

public record PagedResponse<T>(
    List<T> items,
    int offset,
    int limit,
    int total) {
}
