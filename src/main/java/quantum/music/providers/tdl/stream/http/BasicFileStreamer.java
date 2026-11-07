package quantum.music.providers.tdl.stream.http;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
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
        return Multi.createFrom().emitter(emitter -> httpClient.request(options)
            .onItem().transformToUni(req -> req.send())
            .subscribe().with(resp -> emit(resp, emitter) , emitter::fail));
    }

    private void emit(HttpClientResponse resp, MultiEmitter<? super Buffer> emitter) {
        if (resp.statusCode() != 200) {
            emitter.fail(new WebApplicationException("Failed: " + resp.statusCode(), resp.statusCode()));
            return;
        }
        resp.handler(emitter::emit);
        resp.endHandler(emitter::complete);
        resp.exceptionHandler(emitter::fail);
    }

}
