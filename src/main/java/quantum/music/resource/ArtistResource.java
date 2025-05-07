package quantum.music.resource;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import quantum.music.dto.detail.ArtistDetail;
import quantum.music.dto.summary.Album;
import quantum.music.dto.summary.ItemsResponse;
import quantum.music.service.AlbumService;
import quantum.music.service.ArtistService;
import quantum.music.service.TokenService;

@Path("/music")
@Produces(MediaType.APPLICATION_JSON)
public class ArtistResource {

    @Inject
    ArtistService artistService;

    @Inject
    AlbumService albumService;

    @Inject
    TokenService tokenService;

    @GET
    @Path("/tdl/artists/{artist}")
    public Uni<ArtistDetail> artist(@PathParam("artist") String artist) {
        return tokenService.withToken(() -> artistService.artist(artist));
    }

    @GET
    @Path("/tdl/artists/{artist}/albums")
    public Uni<ItemsResponse<Album>> albums(
            @PathParam("artist") String artist,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("10") int limit
    ) {
        return tokenService.withToken(() -> albumService.albums(artist, offset, limit));
    }
}
