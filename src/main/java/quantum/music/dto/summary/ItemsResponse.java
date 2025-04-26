package quantum.music.dto.summary;

import java.util.List;

public record ItemsResponse<T>(List<T> items, int total, int offset, int limit) {

}
