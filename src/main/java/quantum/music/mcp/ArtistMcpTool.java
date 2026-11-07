package quantum.music.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import quantum.music.service.ProviderService;

import java.util.List;
import java.util.Optional;

/// Exposes artist search as a tool on the application's MCP server.
@ApplicationScoped
public class ArtistMcpTool {

    @Inject
    ProviderService providerService;

    @Inject
    ObjectMapper objectMapper;

    @Tool(name = "search_artists", description = "Search artists by text query and return paginated results")
    public Uni<String> searchArtists(
            @ToolArg(name = "query", description = "Artist search query") String query,
            @ToolArg(name = "source", description = "Provider id (tdl or lcl)", required = false)
            Optional<String> source,
            @ToolArg(name = "offset", description = "Pagination offset", required = false)
            Optional<Integer> offset,
            @ToolArg(name = "limit", description = "Page size", required = false)
            Optional<Integer> limit
    ) {
        String provider = source.filter(s -> !s.isBlank()).orElse("lcl");
        int parsedOffset = offset.orElse(0);
        int parsedLimit = limit.orElse(10);
        return providerService.getProvider(provider)
            .onItem().transformToUni(musicProvider -> musicProvider.searchArtists(query, parsedOffset, parsedLimit))
            .onItem().transform(page -> {
                List<ArtistItem> artists = page.items().stream()
                    .map(artist -> new ArtistItem(artist.id(), artist.name(), artist.picture()))
                    .toList();
                return writeJson(new ArtistSearchResult(provider, page.offset(), page.limit(), page.total(), artists));
            });
    }

    private String writeJson(ArtistSearchResult payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize artist search response", e);
        }
    }

    private record ArtistItem(String id, String name, String picture) {
    }

    private record ArtistSearchResult(String source, int offset, int limit, int total, List<ArtistItem> items) {
    }
}
