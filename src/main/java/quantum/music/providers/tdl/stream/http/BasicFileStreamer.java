package quantum.music.providers.tdl.stream.http;

import io.smallrye.mutiny.Multi;
import io.vertx.core.http.RequestOptions;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientResponse;
import jakarta.ws.rs.WebApplicationException;
import quantum.music.providers.tdl.stream.FileStreamer;

/// Streams the response body of a single HTTP request.
public class BasicFileStreamer implements FileStreamer {

    private final HttpClient httpClient;
    private final RequestOptions options;

    /// Creates an HTTP streamer with the supplied client and request options.
    ///
    /// @param httpClient client used to make the request
    /// @param options request method and target URL
    public BasicFileStreamer(HttpClient httpClient, RequestOptions options) {
        this.httpClient = httpClient;
        this.options = options;
    }

    @Override
    public Multi<Buffer> stream() {
        return httpClient.request(options)
                .onItem().transformToUni(request -> request.send())
                .onItem().invoke(HttpClientResponse::pause)
                .onItem().transformToMulti(BasicFileStreamer::emitBuffers);
    }

    private static Multi<Buffer> emitBuffers(HttpClientResponse response) {
        if (response.statusCode() != 200) {
            response.resume();
            return Multi.createFrom()
                .failure(new WebApplicationException("Failed: " + response.statusCode(), response.statusCode()));
        }
        return response.toMulti();
    }

}
