package quantum.music.providers.tdl.stream.http;

import io.smallrye.mutiny.Multi;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import quantum.music.providers.tdl.stream.FileStreamer;

import java.util.List;

/// Streams a sequence of URLs, concatenating their audio data in order.
public class MultiUrlFileStreamer implements FileStreamer {

    private final HttpClient httpClient;
    private final List<String> urls;

    /// Creates a streamer for the supplied media URLs.
    ///
    /// @param httpClient client used for each request
    /// @param urls media URLs to stream in order
    public MultiUrlFileStreamer(HttpClient httpClient, List<String> urls) {
        this.httpClient = httpClient;
        this.urls = urls;
    }

    @Override
    public Multi<Buffer> stream() {
        if (urls == null || urls.isEmpty()) {
            return Multi.createFrom().failure(new IllegalArgumentException("At least one URL is required."));
        }

        return Multi.createFrom().iterable(urls).onItem()
            .transformToMultiAndConcatenate(url -> build(url).stream());
    }

    private BasicFileStreamer build(String url) {
        return new BasicFileStreamer(
            httpClient,
            new RequestOptions().setMethod(HttpMethod.GET).setAbsoluteURI(url)
        );
    }
}
