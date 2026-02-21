package quantum.music.providers.tdl.services;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import quantum.music.client.ApiClient;
import quantum.music.domain.providers.Artist;
import quantum.music.service.TokenService;

@ApplicationScoped
public class TdlArtistService extends TldAbstractService {

    @Inject
    @RestClient
    private ApiClient client;

    @Inject
    TokenService tokenService;

    public Uni<Artist> getArtistById(String artistId) {
        return tokenService.withToken(() -> {
                    Uni<JsonObject> artist = client.artist(parsedId(artistId));
                    Uni<JsonObject> bio = client.bio(parsedId(artistId))
                            .onFailure().recoverWithItem(new JsonObject().put("text", ""));
                    return Uni.combine().all().unis(artist, bio).asTuple().map(this::map);
                });
    }

    private Artist map(Tuple2<JsonObject, JsonObject> tuple) {
        JsonObject artistJson = tuple.getItem1();
        JsonObject bioJson = tuple.getItem2();
        return Artist.builder()
                .id(formatId(artistJson.getLong("id")))
                .name(artistJson.getString("name"))
                .bio(bioJson.getString("text").replaceAll("\\[.*?\\]", ""))
                .build();
    }

}
