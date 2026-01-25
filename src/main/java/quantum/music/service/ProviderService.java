package quantum.music.service;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple3;
import jakarta.enterprise.context.ApplicationScoped;
import quantum.music.providers.MusicProvider;
import quantum.music.providers.lcl.LclMusicProvider;
import quantum.music.providers.tdl.TdlMusicProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotFoundException;

/**
 * Service for managing and retrieving music providers by their IDs.
 * <p>
 * Provides methods to register and fetch {@link MusicProvider} instances
 * based on provider or resource IDs. Returns 404 (NotFoundException) if a provider is not found.
 */
@ApplicationScoped
public class ProviderService {

    private final Map<String, MusicProvider> providers = new HashMap<>();

    /**
     * Constructs the ProviderService and registers available music providers.
     *
     * @param tdlMusicProvider the TDL music provider instance
     * @param lclMusicProvider the LCL music provider instance
     */
    public ProviderService(TdlMusicProvider tdlMusicProvider, LclMusicProvider lclMusicProvider) {
        providers.put(tdlMusicProvider.getProviderId(), tdlMusicProvider);
        providers.put(lclMusicProvider.getProviderId(), lclMusicProvider);
    }

    /**
     * Retrieves a music provider based on a resource ID in the format "providerId:resourceId".
     *
     * @param resourceId the resource ID string
     * @return a Uni emitting the found MusicProvider, or failing with NotFoundException if not found
     * @throws IllegalArgumentException if the resource ID format is invalid
     */
    public Uni<MusicProvider> fromId(String resourceId) {
        String[] parts = resourceId.split(":", 2);
        if (parts.length != 2) {
            return Uni.createFrom().failure(new IllegalArgumentException("Invalid resource ID format"));
        }
        String providerId = parts[0];
        MusicProvider provider = providers.get(providerId);
        if (provider == null) {
            return Uni.createFrom().failure(new NotFoundException(STR."Unknown provider ID: \{providerId}"));
        }
        return Uni.createFrom().item(provider);
    }

    /**
     * Retrieves a music provider by its provider ID.
     *
     * @param providerId the provider ID
     * @return a Uni emitting the found MusicProvider, or failing with NotFoundException if not found
     */
    public Uni<MusicProvider> getProvider(String providerId) {
        MusicProvider provider = providers.get(providerId);
        if (provider == null) {
            return Uni.createFrom().failure(new NotFoundException(STR."Unknown provider ID: \{providerId}"));
        }
        return Uni.createFrom().item(provider);
    }

    public List<Tuple3<String,String, List<String>>> getProviders() {
        return providers.values().stream()
                .map(provider -> Tuple3.of(provider.getProviderId(), provider.getProviderName(), provider.getCapabilities()))
                .toList();
    }

}
