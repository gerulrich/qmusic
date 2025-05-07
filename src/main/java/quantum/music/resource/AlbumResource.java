package quantum.music.resource;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import quantum.music.dto.detail.AlbumDetail;
import quantum.music.dto.summary.Album;
import quantum.music.dto.summary.ItemsResponse;
import quantum.music.dto.summary.TrackList;
import quantum.music.service.AlbumService;
import quantum.music.service.TokenService;
import quantum.music.service.TrackService;

@Path("/music")
@Produces(MediaType.APPLICATION_JSON)
public class AlbumResource {

    @Inject
    AlbumService albumService;

    @Inject
    TrackService trackService;

    @Inject
    TokenService tokenService;

    @GET
    @Path("/tdl/search")
    public Uni<ItemsResponse<Album>> search(
            @QueryParam("q") String query,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("10") int limit
    ) {
        return tokenService.withToken(() -> albumService.search(query, offset, limit));
    }

    @GET
    @Path("/tdl/albums/{id}")
    public Uni<AlbumDetail> album(@PathParam("id") String album) {
        return tokenService.withToken(() -> albumService.album(album));
    }

    @GET
    @Path("/tdl/albums/{id}/tracks")
    public Uni<TrackList> tracks(@PathParam("id") String album) {
        return tokenService.withToken(() -> trackService.tracks(album));
    }
}
