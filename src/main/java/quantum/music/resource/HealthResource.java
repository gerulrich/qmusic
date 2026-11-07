package quantum.music.resource;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/// Resource providing health check and version information endpoints.
///
/// This resource offers simple REST endpoints to verify application health status
/// and to retrieve the current version of the application. These endpoints are typically
/// used by monitoring systems, load balancers, and deployment pipelines to verify
/// the system's operational status.
///
/// The health endpoint returns a simple status string, while the version endpoint
/// returns the application version configured via microprofile config properties.
@Path("/")
public class HealthResource {

    /// The application version retrieved from configuration properties.
    @ConfigProperty(name = "app.version")
    String appVersion;

    /// Health check endpoint that verifies the application is running.
    ///
    /// This endpoint returns a simple "OK" string when the application is operational.
    /// It can be used by monitoring systems and load balancers to determine if the
    /// service is healthy and available to handle requests.
    ///
    /// @return The string "OK" indicating the application is running properly
    @GET
    @Path("/health")
    @Operation(hidden = true)
    public String health() {
        return "OK";
    }

    /// Version information endpoint.
    ///
    /// Returns the current version of the application as defined in the configuration.
    /// This endpoint is useful for verification during deployment pipelines and for
    /// support teams to identify which version is running in a specific environment.
    ///
    /// @return The application version string from configuration properties
    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(hidden = true)
    public String version() {
        return appVersion;
    }

}