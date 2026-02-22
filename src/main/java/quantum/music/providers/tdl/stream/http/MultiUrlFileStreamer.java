package quantum.music.providers.tdl.stream.http;

import io.smallrye.mutiny.Multi;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import quantum.music.providers.tdl.stream.FileStreamer;

import java.util.List;

public class MultiUrlFileStreamer implements FileStreamer {

    private final HttpClient httpClient;
    private final List<String> urls;

    public MultiUrlFileStreamer(HttpClient httpClient, List<String> urls) {
        this.httpClient = httpClient;
        this.urls = urls;
    }

    @Override
    public Multi<Buffer> stream() {
        if (urls == null || urls.isEmpty()) {
            return Multi.createFrom().empty();
        }

        return Multi.createFrom().iterable(urls)
            .onItem().transformToMultiAndConcatenate(url -> new BasicFileStreamer(
                httpClient,
                new RequestOptions().setMethod(HttpMethod.GET).setAbsoluteURI(url)
            ).stream());
    }
}

