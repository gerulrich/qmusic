package quantum.music.resource;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import quantum.music.api.ApiAlbum;
import quantum.music.api.ApiArtist;
import quantum.music.domain.providers.Artist;
import quantum.music.domain.providers.Album;
import quantum.music.api.ItemsResponse;
import quantum.music.service.ProviderService;

import java.util.List;
import java.util.stream.Collectors;

@Path("/music")
@Produces(MediaType.APPLICATION_JSON)
public class SearchResource {

    @Inject
    ProviderService providerService;

    @GET
    @Path("/search")
    public Uni<ItemsResponse<ApiAlbum>> search(
            @Context UriInfo uriInfo,
            @QueryParam("provider") @DefaultValue("tdl") String provider,
            @QueryParam("q") String query,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("10") int limit
    ) {
        String baseUrl = uriInfo.getBaseUri().toString().replaceAll("/$", "/music");

        return providerService.getProvider(provider) // select provider
            .onItem().transformToUni(musicProvider -> musicProvider.search(query, offset, limit))
            .onItem().transform(pagedResponse -> {
                    List<ApiAlbum> albums = pagedResponse.items().stream()
                            .map(album -> map(album, baseUrl))
                            .collect(Collectors.toList());
                    return new ItemsResponse<>(albums, pagedResponse.total(), pagedResponse.offset(), pagedResponse.limit());
                });
    }

    private ApiAlbum map(Album album, String baseUrl) {
        String albumLink = album.link() != null ? baseUrl + album.link() : null;

        return new ApiAlbum(
                album.id(),
                album.title(),
                album.release(),
                map(album.artist(), baseUrl),
                album.cover(),
                null,
                null,
                null,
                albumLink,
                album.type()
        );
    }

    private ApiArtist map(Artist artist, String baseUrl) {
        String artistLink = artist.link() != null ? baseUrl + artist.link() : null;

        return new ApiArtist(
                artist.id(),
                artist.name(),
                artistLink
        );
    }

}
