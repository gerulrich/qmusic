package quantum.music.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@DisplayName("Health Resource Tests")
public class HealthResourceTest {

    @Test
    @DisplayName("Health check endpoint should return OK status and correct content type")
    public void testHealthCheck() {
        given()
                .when().get("/health")
                .then()
                .statusCode(200)
                .body(is("OK"))
                .contentType("text/plain;charset=UTF-8");
    }

    @Test
    @DisplayName("Version endpoint should return configured version  and correct content type")
    public void testVersionEndpoint() {
        given()
                .when().get("/version")
                .then()
                .statusCode(200)
                .body(is("test"))
                .contentType("text/plain;charset=UTF-8");
    }
}