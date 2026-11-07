package quantum.music.providers.tdl.stream;

import io.smallrye.mutiny.Multi;
import io.vertx.mutiny.core.buffer.Buffer;

/// Supplies audio data as a reactive stream of buffers.
public interface FileStreamer {

    /// Starts streaming audio data.
    ///
    /// @return the stream of audio buffers
    Multi<Buffer> stream();
}
