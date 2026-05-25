package io.github.jayesh1126.supabase.auth;

import io.github.jayesh1126.supabase.auth.model.AuthResponse;
import io.github.jayesh1126.supabase.auth.model.User;
import io.github.jayesh1126.supabase.core.SupabaseHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class AuthClientTest {

    private SupabaseHttpClient http;
    private ObjectMapper objectMapper;

    private AuthClient authClient;

    @BeforeEach
    void setUp() {
        http = mock(SupabaseHttpClient.class);
        objectMapper = new ObjectMapper();

        authClient = new AuthClient(
                http,
                objectMapper,
                "access-token"
        );
    }

    @Test
    void shouldCreateAuthClient() {
        assertNotNull(authClient);
    }

    @Test
    void shouldThrowWhenHttpClientIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AuthClient(
                        null,
                        objectMapper,
                        "token"
                )
        );

        assertEquals(
                "http must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenObjectMapperIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new AuthClient(
                        http,
                        null,
                        "token"
                )
        );

        assertEquals(
                "objectMapper must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenSignUpEmailIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authClient.signUpWithEmail(
                        null,
                        "password"
                )
        );

        assertEquals(
                "email must not be null or blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenSignUpPasswordIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authClient.signUpWithEmail(
                        "test@example.com",
                        " "
                )
        );

        assertEquals(
                "password must not be null or blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenSignInEmailIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authClient.signInWithEmail(
                        "",
                        "password"
                )
        );

        assertEquals(
                "email must not be null or blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenRefreshTokenIsBlank() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authClient.refreshAccessToken(" ")
        );

        assertEquals(
                "refreshToken must not be null or blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenGettingUserWithoutAccessToken() {
        AuthClient unauthenticatedClient =
                new AuthClient(
                        http,
                        objectMapper,
                        null
                );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                unauthenticatedClient::getUser
        );

        assertTrue(
                exception.getMessage().contains("No access token present")
        );
    }

    @Test
    void shouldThrowWhenSigningOutWithoutAccessToken() {
        AuthClient unauthenticatedClient =
                new AuthClient(
                        http,
                        objectMapper,
                        ""
                );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                unauthenticatedClient::signOut
        );

        assertTrue(
                exception.getMessage().contains("No access token present")
        );
    }

    @Test
    void shouldFetchAuthenticatedUser() throws Exception {

        String userJson = """
                {
                    "id": "123",
                    "email": "test@example.com"
                }
                """;

        when(http.get(
                eq("/auth/v1/user"),
                isNull(),
                isNull()
        )).thenReturn(userJson);

        User user = authClient.getUser();

        assertNotNull(user);

        verify(http).get(
                eq("/auth/v1/user"),
                isNull(),
                isNull()
        );
    }

    @Test
    void shouldCallLogoutEndpointOnSignOut() throws Exception {

        when(http.post(
                eq("/auth/v1/logout"),
                isNull(),
                anyString(),
                isNull()
        )).thenReturn("");

        assertDoesNotThrow(() -> authClient.signOut());

        verify(http).post(
                eq("/auth/v1/logout"),
                isNull(),
                anyString(),
                isNull()
        );
    }

    @Test
    void shouldWrapErrorsWhenFetchingUserFails() throws Exception {

        when(http.get(
                anyString(),
                any(),
                any()
        )).thenThrow(new RuntimeException("HTTP failure"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                authClient::getUser
        );

        assertTrue(
                exception.getMessage()
                        .contains("Failed to fetch authenticated user")
        );
    }

    @Test
    void shouldWrapErrorsWhenSignOutFails() throws Exception {

        when(http.post(
                anyString(),
                any(),
                anyString(),
                any()
        )).thenThrow(new RuntimeException("HTTP failure"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                authClient::signOut
        );

        assertTrue(
                exception.getMessage()
                        .contains("Failed to sign out user")
        );
    }
}