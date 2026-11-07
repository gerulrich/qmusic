package quantum.music.providers.tdl.services;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import quantum.music.client.ApiClient;
import quantum.music.domain.providers.Artist;
import quantum.music.service.TokenService;

/// Retrieves and maps artist details from the TDL API.
@ApplicationScoped
public class TdlArtistService extends TldAbstractService {

    @Inject
    @RestClient
    private ApiClient client;

    @Inject
    TokenService tokenService;

    /// Gets an artist, including the artist biography when available.
    ///
    /// @param artistId provider-specific artist identifier
    /// @return the artist details
    public Uni<Artist> getArtistById(String artistId) {
        return tokenService.withToken(() -> fetch(artistId));
    }

    private Uni<Artist> fetch(String artistId) {
        Uni<JsonObject> bio = client.bio(parsedId(artistId))
                .onFailure()
                .recoverWithItem(new JsonObject().put("text", ""));
        Uni<JsonObject> artist = client.artist(parsedId(artistId));
        return Uni.combine().all().unis(artist, bio).with(this::map);
    }

    private Artist map(JsonObject artist, JsonObject bio) {
        return Artist.builder()
            .id(formatId(artist.getLong("id")))
            .name(artist.getString("name"))
            .bio(bio.getString("text").replaceAll("\\[.*?\\]", ""))
            .picture(formatImageUrl(artist.getString("picture"), ARTIST_RESOLUTION))
            .build();
    }
}
