package quantum.music.providers.tdl.stream.http;

import io.smallrye.mutiny.Multi;
import io.vertx.core.http.RequestOptions;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import jakarta.ws.rs.WebApplicationException;
import quantum.music.providers.tdl.stream.FileStreamer;

public class BasicFileStreamer implements FileStreamer {

    private final HttpClient httpClient;
    private final RequestOptions options;

    public BasicFileStreamer(HttpClient httpClient, RequestOptions options) {
        this.httpClient = httpClient;
        this.options = options;
    }

    @Override
    public Multi<Buffer> stream() {
        return Multi.createFrom().emitter(emitter -> httpClient.request(options)
            .onItem().transformToUni(req -> req.send())
            .subscribe().with(resp -> {
                if (resp.statusCode() != 200) {
                    emitter.fail(new WebApplicationException("Failed: " + resp.statusCode(), resp.statusCode()));
                    return;
                }
                resp.handler(emitter::emit);
                resp.endHandler(emitter::complete);
                resp.exceptionHandler(emitter::fail);
            }, emitter::fail));
    }
}

