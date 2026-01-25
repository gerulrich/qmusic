package quantum.music.providers.lcl.services;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import quantum.music.domain.providers.Artist;
import quantum.music.repository.ArtistRepository;

/**
 * Service for local artist lookups.
 *
 * <p>Provides provider-facing retrieval logic for artists.</p>
 */
@ApplicationScoped
public class LclArtistService extends LclProviderService {

    private static final Logger LOG = Logger.getLogger(LclArtistService.class);

    @Inject
    private ArtistRepository repository;

    /**
     * Retrieves a local artist by its provider-facing id.
     *
     * @param artistId provider-facing artist id
     * @return artist data for the requested id
     * @throws NotFoundException when the artist does not exist
     */
    public Uni<Artist> getArtistById(String artistId) {
        LOG.debugf("Fetching local artist details for id=%s", artistId);
        return repository.findById(new ObjectId(parsedId(artistId)))
            .onItem().ifNull().failWith(() -> new NotFoundException(STR."Artist not found: \{artistId}"))
            .onItem().transform(artist -> Artist.builder().id(formatId(artist.id)).name(artist.name).build());
    }

}
