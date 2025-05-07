package quantum.music.dto.summary;

import java.util.List;

public record ItemsResponse<T>(List<T> items, int offset, int limit, int total) {

}
