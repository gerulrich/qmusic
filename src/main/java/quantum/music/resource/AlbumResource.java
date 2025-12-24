package quantum.music.resource;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import quantum.music.api.*;
import quantum.music.service.ProviderService;

@Path("/music")
@Produces(MediaType.APPLICATION_JSON)
public class AlbumResource {

    @Inject
    ProviderService providerService;

    @GET
    @Path("/albums/{id}")
    public Uni<ApiAlbum> album(@Context UriInfo uriInfo, @PathParam("id") String id) {
        return providerService.fromId(id)
                .onItem().transformToUni(musicProvider -> musicProvider.getAlbumById(id))
                .onItem().transform( album -> {
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
                            STR."\{baseUrl}\{album.link()}/tracks",
                            null,
                            album.type()
                    );
                });
    }

    @GET
    @Path("/albums/{id}/tracks")
    public Uni<ApiAlbumTracks> tracks(@Context UriInfo uriInfo, @PathParam("id") String id) {
        return providerService.fromId(id)
                .onItem().transformToUni(musicProvider -> musicProvider.getTracksByAlbumId(id))
                .onItem().transform( album -> {
                    String baseUrl = getBaseUrl(uriInfo);
                    return new ApiAlbumTracks(
                            null,
                            album.tracks().size(),
                            album.tracks().stream().map(track -> new ApiTrack(
                                    track.id(),
                                    track.title(),
                                    track.duration(),
                                    track.trackNumber(),
                                    track.volumeNumber(),
                                    null,
                                    baseUrl + track.link()
                            )).toList()
                    );
                });
    }

    private String getBaseUrl(UriInfo uriInfo) {
        return uriInfo.getBaseUri().toString().replaceAll("/$", "/music");
    }
}
