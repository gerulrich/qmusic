package quantum.music.mcp;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import quantum.music.service.ArtistSearchService;

import java.util.List;

/// Exposes semantic artist search as a tool on the application's MCP server.
@ApplicationScoped
public class ArtistMcpTool {

    @Inject
    ArtistSearchService artistSearchService;

    @Tool(name = "search_artists", description = "Search artists by semantic similarity to a text query")
    public Uni<ArtistSearchResult> searchArtists(@ToolArg(name = "query", description = "Artist search query") String query) {
        return artistSearchService.search(query)
            .onItem().transform(artists -> new ArtistSearchResult(
                artists.stream()
                    .map(artist -> new ArtistItem(artist.id.toString(), artist.name, artist.bio))
                    .toList()
            ));
    }

    record ArtistItem(String id, String artist, String bio) {
    }

    record ArtistSearchResult(List<ArtistItem> items) {
    }
}
