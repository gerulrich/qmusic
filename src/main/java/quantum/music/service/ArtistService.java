package quantum.music.service;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import quantum.music.client.ApiClient;
import quantum.music.dto.detail.ArtistDetail;

import static java.lang.String.format;

@ApplicationScoped
public class ArtistService {

    private static final Logger LOG = Logger.getLogger(ArtistService.class);

    @Inject
    @RestClient
    private ApiClient apiClient;

    @ConfigProperty(name = "app.domain")
    private String domain;

    /**
     * Get artist details by ID.
     *
     * @param artist Artist ID
     * @return Artist details
     */
    public Uni<ArtistDetail> artist(String artist) {
        return Uni.combine().all().unis(apiClient.artist(artist), apiClient.bio(artist))
                .asTuple()
                .onItem().transform(tuple -> {
                    JsonObject json = tuple.getItem1();
                    JsonObject bio = tuple.getItem2();
                    return new ArtistDetail(
                        json.getLong("id"),
                        json.getString("name"),
                        formatResourceUrl(json.getLong("id"), "albums"),
                        bio.getString("text")
                    );
                })
                .onFailure().invoke(e -> LOG.errorf(e, "Error getting artist: %s", artist));
    }

    /**
     * Get artist details by ID.
     *
     * @param id Artist ID
     * @return Artist details
     */
    private String formatResourceUrl(Long id, String... additionalPaths) {
        StringBuilder builder = new StringBuilder(format("%s/artists/%s", domain, id));
        for (String path : additionalPaths) {
            builder.append("/").append(path);
        }
        return builder.toString();
    }

}
