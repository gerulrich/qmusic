package quantum.music.service;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import quantum.music.client.OAuth2Client;
import quantum.music.model.TokenResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static quantum.music.utils.ReflectionUtils.setValue;

@ExtendWith(MockitoExtension.class)
public class TokenServiceTest {

    @InjectMocks
    TokenService tokenService;

    @Mock
    OAuth2Client oAuth2Client;

    @BeforeEach
    void setup() throws Exception {
        // Set the config properties using reflection
        setValue(tokenService, "clientId", "test-client-id");
        setValue(tokenService, "refreshToken", "test-refresh-token");
        // Create a test token with 1 hour expiry
        Mockito.when(oAuth2Client.renewToken(
                eq("refresh_token"),
                anyString(),
                anyString(),
                eq("r_usr+w_usr")
        )).thenReturn(
                Uni.createFrom()
                        .item(new TokenResponse(
                                "test-access-token",
                                "refresh-token",
                                3600L)
                        )
        );
    }

    @Test
    void testGetToken_ShouldRenewOnFirstCall() {
        String token = tokenService.getToken().await().indefinitely();

        assertEquals("test-access-token", token);
        verify(oAuth2Client, times(1)).renewToken(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testGetToken_ShouldUseCachedToken() {
        // First call to cache token and reset mock counts
        tokenService.getToken().await().indefinitely();
        clearInvocations(oAuth2Client);

        // Second call should use cached token
        String token = tokenService.getToken().await().indefinitely();

        assertEquals("test-access-token", token);
        verify(oAuth2Client, never()).renewToken(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testGetToken_ShouldRenewWhenExpired() throws Exception {
        // First get token, manually set expiry to past and reset mock counts
        tokenService.getToken().await().indefinitely();
        setValue(tokenService, "expiresAt", Instant.now().minus(1, ChronoUnit.HOURS));
        clearInvocations(oAuth2Client);

        // Should renew token when expired
        tokenService.getToken().await().indefinitely();

        verify(oAuth2Client, times(1))
                .renewToken(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString()
                );
    }

    @Test
    void testWithToken_ShouldExecuteFunction() {
        // Set up a function to be called with the token
        String result = tokenService.withToken(() ->
                Uni.createFrom().item("function-result")
        ).await().indefinitely();

        assertEquals("function-result", result);
        assertNotNull(tokenService.getCachedToken());
        verify(oAuth2Client, times(1)).renewToken(anyString(), anyString(), anyString(), anyString());
    }
}