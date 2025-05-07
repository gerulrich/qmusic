package quantum.music.resource;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import quantum.music.dto.detail.MediaInfo;
import quantum.music.dto.detail.TrackDetail;
import quantum.music.service.TokenService;
import quantum.music.service.TrackService;

@Path("/music")
@Produces(MediaType.APPLICATION_JSON)
public class TrackResource {

    @Inject
    TrackService trackService;

    @Inject
    TokenService tokenService;

    @GET
    @Path("/tdl/tracks/{id}")
    public Uni<TrackDetail> track(@PathParam("id") String track) {
        return tokenService.withToken(() -> trackService.track(track));
    }

    @GET
    @Path("/tdl/tracks/{id}/content")
    public Uni<MediaInfo> content(@PathParam("id") String track) {
        return tokenService.withToken(() -> trackService.content(track));
    }
}
