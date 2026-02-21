package quantum.music.providers.lcl.services;

import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import quantum.music.domain.PagedResponse;
import quantum.music.domain.local.QAlbum;
import quantum.music.domain.providers.Album;
import quantum.music.domain.providers.Artist;
import quantum.music.repository.AlbumRepository;

import java.util.List;

/**
 * Service responsible for text-based search of locally stored albums.
 *
 * <p>Runs a Mongo text query through the {@link AlbumRepository} and maps
 * {@link quantum.music.domain.local.QAlbum} entities into provider-facing
 * {@link Album} DTOs, including basic artist data and source tags.</p>
 */
@ApplicationScoped
public class LclSearchService extends LclProviderService {

    private static final Logger LOG = Logger.getLogger(LclSearchService.class);
    private static final String QUERY_TEMPLATE = "{ $text: { $search: :search } }";

    @Inject
    AlbumRepository repository;

    /**
     * Executes a text search over local albums.
     *
     * @param q search query text (must be non-blank to perform a search)
     * @param offset zero-based item offset into the full result set
     * @param limit maximum number of items to return
     * @return paged list of albums matching the query
     */
    public Uni<PagedResponse<Album>> search(String q, int offset, int limit) {
        if (q == null || q.isBlank()) {
            LOG.debugf("LCL search skipped: blank query (offset=%d, limit=%d)", offset, limit);
            return Uni.createFrom().item(new PagedResponse<>(List.of(), offset, limit, 0));
        }
        int page = pageIndex(offset, limit);
        LOG.debugf("LCL search started: query='%s', offset=%d, limit=%d, page=%d", q, offset, limit, page);
        Parameters parameters = Parameters.with("search", q);
        return Uni.combine().all().unis(
                repository.find(QUERY_TEMPLATE, parameters).page(page, limit).list(),
                repository.count(QUERY_TEMPLATE, parameters)
            )
            .asTuple()
            .onItem().invoke(tuple -> LOG.debugf("LCL search fetched: items=%d, total=%d", tuple.getItem1().size(), tuple.getItem2()))
            .onItem().transform(tuple -> map(offset, limit, tuple));
    }

    /** Maps persistence entities to provider albums and wraps them in a paged response. */
    private PagedResponse<Album> map(int offset, int limit, Tuple2<List<QAlbum>, Long> tuple) {
        int totalCount = tuple.getItem2().intValue();
        LOG.debugf("LCL search mapped: items=%d, total=%d", tuple.getItem1().size(), totalCount);
        return new PagedResponse<>(
                mapList(tuple.getItem1(), album -> Album.builder()
                    .id(formatId(album.id))
                    .title(album.title)
                    .artist(
                        Artist.builder()
                            .id(formatId(album.artistId))
                            .name(album.artist)
                        .build()
                    )
                    .release(album.release)
                    .copyright(album.copyright)
                    .type("ALBUM")
                    .cover(album.cover)
                    .tags(sourceTags(album.source))
                .build()),
                offset,
                limit, totalCount);
    }
}
