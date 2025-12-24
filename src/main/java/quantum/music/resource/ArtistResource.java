package quantum.music.resource;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import quantum.music.api.*;
import quantum.music.service.ProviderService;

import java.util.List;
import java.util.stream.Collectors;

@Path("/music")
@Produces(MediaType.APPLICATION_JSON)
public class ArtistResource {

    @Inject
    ProviderService providerService;

    @GET
    @Path("/artists/{artist}/albums")
    public Uni<ItemsResponse<ApiAlbum>> albums(
            @Context UriInfo uriInfo,
            @PathParam("artist") String artist,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("10") int limit
    ) {
        return providerService.fromId(artist)
                .onItem().transformToUni(musicProvider -> musicProvider.getAlbumsByArtistId(artist, offset, limit))
                .onItem().transform(pagedResponse -> {
                    List<ApiAlbum> albums = pagedResponse.items().stream()
                            .map(album -> {
                                String baseUrl = getBaseUrl(uriInfo);
                                return new ApiAlbum(
                                        album.id(),
                                        album.title(),
                                        album.release(),
                                        new ApiArtist(
                                                album.artist().id(),
                                                album.artist().name(),
                                                baseUrl + album.artist().link()
                                        ),
                                        album.cover(),
                                        album.copyright(),
                                        null, // TODO completar tags,
                                        baseUrl + album.link(),
                                        album.type()
                                );
                            })
                            .collect(Collectors.toList());
                    return new quantum.music.api.ItemsResponse<>(albums, pagedResponse.total(), pagedResponse.offset(), pagedResponse.limit());
                });
    }

    private String getBaseUrl(UriInfo uriInfo) {
        return uriInfo.getBaseUri().toString().replaceAll("/$", "/music");
    }
}
