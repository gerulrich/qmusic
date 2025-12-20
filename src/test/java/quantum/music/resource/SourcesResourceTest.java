package quantum.music.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;

/**
 * Integration tests for {@link SourcesResource}.
 */
@QuarkusTest
class SourcesResourceTest {

    /**
     * Test that the GET /music/sources endpoint returns HTTP 200
     * and the expected audio sources.
     */
    @Test
    void testListSourcesEndpoint() {
        given()
            .when().get("/music/sources")
            .then()
                .statusCode(200)
                .contentType("application/json")
                .body("$", hasSize(2))
                .body("[0].name", is("local"))
                .body("[1].name", is("tdl"));
    }
}
