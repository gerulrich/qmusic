package quantum.music.resource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;
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

    @GET
    @Path("/tdl/tracks/{id}/stream")
    @Produces("audio/flac")
    @RestStreamElementType(MediaType.APPLICATION_OCTET_STREAM)
    public Multi<Buffer> stream(@PathParam("id") String id) {
        return  tokenService
                .withToken(() -> trackService.content(id))
                .onItem().transformToMulti(mi -> trackService.proxyFile(mi.url(), mi.encryption()));
    }
}
