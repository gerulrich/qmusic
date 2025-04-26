package quantum.music.resource;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import quantum.music.dto.detail.*;
import quantum.music.dto.summary.Album;
import quantum.music.dto.summary.ItemsResponse;
import quantum.music.dto.summary.TrackList;
import quantum.music.service.MusicService;
import quantum.music.service.TokenService;

@Path("/music")
@Produces(MediaType.APPLICATION_JSON)
public class MusicResource {

    @Inject
    MusicService musicService;

    @Inject
    TokenService tokenService;

    @GET
    @Path("/tdl/search")
    public Uni<ItemsResponse<Album>> search(
            @QueryParam("q") String query,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("10") int limit
    ) {
        return tokenService.withToken(() -> musicService.search(query, offset, limit));
    }

    @GET
    @Path("/tdl/artists/{artist}")
    public Uni<ArtistDetail> artist(@PathParam("artist") String artist) {
        return tokenService.withToken(() -> musicService.artist(artist));
    }

    @GET
    @Path("/tdl/artists/{artist}/albums")
    public Uni<ItemsResponse<Album>> albums(
            @PathParam("artist") String artist,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("10") int limit
    ) {
        return tokenService.withToken(() -> musicService.albums(artist, offset, limit));
    }

    @GET
    @Path("/tdl/albums/{id}")
    public Uni<AlbumDetail> album(@PathParam("id") String album) {
        return tokenService.withToken(() -> musicService.album(album));
    }

    @GET
    @Path("/tdl/albums/{id}/tracks")
    public Uni<TrackList> tracks(@PathParam("id") String album) {
        return tokenService.withToken(() -> musicService.tracks(album));
    }

    @GET
    @Path("/tdl/tracks/{id}")
    public Uni<TrackDetail> track(@PathParam("id") String track) {
        return tokenService.withToken(() -> musicService.track(track));
    }

    @GET
    @Path("/tdl/tracks/{id}/content")
    public Uni<MediaInfo> content(@PathParam("id") String track) {
        return tokenService.withToken(() -> musicService.content(track));
    }
}
