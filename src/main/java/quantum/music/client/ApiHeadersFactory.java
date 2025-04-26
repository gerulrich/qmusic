package quantum.music.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import quantum.music.service.TokenService;

@ApplicationScoped
public class ApiHeadersFactory implements ClientHeadersFactory {

    @ConfigProperty(name = "tdl.origin.header")
    String origin;

    @Inject
    TokenService tokenService;

    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders
    ) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.add("Origin", origin);
        result.add("Authorization", String.format("Bearer %s", tokenService.getCachedToken()));
        return result;
    }
}