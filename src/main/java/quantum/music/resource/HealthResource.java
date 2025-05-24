package quantum.music.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/")
public class HealthResource {

    @ConfigProperty(name = "app.version")
    String appVersion;

    @GET
    @Path("/health")
    public String health() {
        return "OK";
    }

    @GET
    @Path("/version")
    public String version() {
        return appVersion;
    }

}