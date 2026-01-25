package quantum.music.providers.tdl.stream;

import io.smallrye.mutiny.Multi;
import io.vertx.mutiny.core.buffer.Buffer;

public interface FileStreamer {
    Multi<Buffer> stream();
}

