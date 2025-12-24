package quantum.music.resource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.RestStreamElementType;
import quantum.music.api.ApiAlbum;
import quantum.music.api.ApiTrack;
import quantum.music.api.ApiTrackStream;
import quantum.music.domain.providers.MediaInfo;
import quantum.music.service.ProviderService;

import java.util.Collections;


@Path("/music")
@Produces(MediaType.APPLICATION_JSON)
public class TrackResource {

    @Inject
    ProviderService providerService;

    @GET
    @Path("/tracks/{id}")
    public Uni<ApiTrack> track(@Context UriInfo uriInfo, @PathParam("id") String id) {
        return providerService.fromId(id)
                .onItem().transformToUni(musicProvider -> musicProvider.getTrackById(id))
                .onItem().transform( track -> new ApiTrack(
                        track.id(),
                        track.title(),
                        track.duration(),
                        track.trackNumber(),
                        track.volumeNumber(),
                        new ApiAlbum(
                                "",
                                "",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "",
                                null
                        ),
                        track.codec(),
                        track.tags(),
                        track.version(),
                        track.copyright(),
                        null,
                        null,

                        // TODO esto deberia ir dentro del provider
                        track.tags().isEmpty() ?
                                Collections.singletonList(new ApiTrackStream(
                                        "AAC",
                                        track.codec(),
                                        getBaseUrl(uriInfo) + STR."/tracks/\{track.id()}/stream?codec=AAC&quality=\{track.codec()}")
                                )
                                :


                                track.tags().stream().map(tag -> new ApiTrackStream(
                                                track.codec(),
                                                tag,
                                                getBaseUrl(uriInfo) + STR."/tracks/\{track.id()}/stream?codec=\{track.codec()}&quality=\{tag}"
                                        )
                                ).toList()
                ));
    }

    @GET
    @Path("/tracks/{id}/content")
    public Uni<MediaInfo> content(@PathParam("id") String track) {
        return null;
    }

    @GET
    @Path("/tracks/{id}/stream")
    @Produces("audio/flac")
    @RestStreamElementType(MediaType.APPLICATION_OCTET_STREAM)
    public Multi<Buffer> stream(
            @PathParam("id") String id,
            @QueryParam("codec") String codec,
            @QueryParam("quality") String quality,
            @QueryParam("presentation") @DefaultValue("FULL") String presentation) {
        return providerService.fromId(id)
                .onItem().transformToMulti(musicProvider -> musicProvider.streamTrackById(id, codec, quality, presentation));
    }

    private String getBaseUrl(UriInfo uriInfo) {
        return uriInfo.getBaseUri().toString().replaceAll("/$", "/music");
    }
}
