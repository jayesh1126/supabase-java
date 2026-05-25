package io.github.jayesh1126.supabase.auth;

import io.github.jayesh1126.supabase.auth.model.AuthResponse;
import io.github.jayesh1126.supabase.auth.model.User;
import io.github.jayesh1126.supabase.core.SupabaseHttpClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Client for Supabase Authentication API.
 *
 * <p>Provides methods for:
 * <ul>
 *     <li>Email/password sign-up</li>
 *     <li>Email/password sign-in</li>
 *     <li>Token refresh</li>
 *     <li>Authenticated user retrieval</li>
 *     <li>Sign-out</li>
 * </ul>
 *
 * <p>Instances are immutable and thread-safe.
 *
 * <p>Typically obtained via:
 * {@link io.github.jayesh1126.supabase.SupabaseClient#auth()}.
 */
public class AuthClient {

    private static final String AUTH_ENDPOINT = "/auth/v1";

    private static final List<Map.Entry<String, String>> PASSWORD_GRANT =
            List.of(Map.entry("grant_type", "password"));

    private static final List<Map.Entry<String, String>> REFRESH_GRANT =
            List.of(Map.entry("grant_type", "refresh_token"));

    private final SupabaseHttpClient http;
    private final ObjectMapper objectMapper;
    private final String accessToken;

    /**
    * Package-private constructor.
    * Instantiated via {@link io.github.jayesh1126.supabase.SupabaseClient}.
    */
    public AuthClient(SupabaseHttpClient http, ObjectMapper objectMapper, String accessToken) {
        Objects.requireNonNull(http, "http must not be null");
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");

        this.http = http;
        this.objectMapper = objectMapper;
        this.accessToken = accessToken;
    }

    /**
     * Signs up a new user using email/password authentication.
     */
    public AuthResponse signUpWithEmail(String email, String password) {
        requireNonBlank(email, "email");
        requireNonBlank(password, "password");

        Map<String, String> body = Map.of(
                "email", email,
                "password", password
        );

        return post(
                "/signup",
                null,
                body,
                AuthResponse.class,
                "Failed to sign up user"
        );
    }

    /**
     * Signs in a user using email/password authentication.
     */
    public AuthResponse signInWithEmail(String email, String password) {
        requireNonBlank(email, "email");
        requireNonBlank(password, "password");

        Map<String, String> body = Map.of(
                "email", email,
                "password", password
        );

        return post(
                "/token",
                PASSWORD_GRANT,
                body,
                AuthResponse.class,
                "Failed to sign in user"
        );
    }

    /**
     * Refreshes an access token using a refresh token.
     */
    public AuthResponse refreshAccessToken(String refreshToken) {
        requireNonBlank(refreshToken, "refreshToken");

        Map<String, String> body = Map.of(
                "refresh_token", refreshToken
        );

        return post(
                "/token",
                REFRESH_GRANT,
                body,
                AuthResponse.class,
                "Failed to refresh access token"
        );
    }

    /**
     * Retrieves the currently authenticated user.
     *
     * @return authenticated user
     * @throws IllegalStateException if no access token is present
     */
    public User getUser() {
        requireAuthenticated();

        try {
            String responseBody = http.get(
                    AUTH_ENDPOINT + "/user",
                    null,
                    null
            );

            return objectMapper.readValue(responseBody, User.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch authenticated user", e);
        }
    }

    /**
     * Signs out the currently authenticated user.
     *
     * @throws IllegalStateException if no access token is present
     */
    public void signOut() {
        requireAuthenticated();

        try {
            http.post(
                    AUTH_ENDPOINT + "/logout",
                    null,
                    "{}",
                    null
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to sign out user", e);
        }
    }

    // ---------------------------------------------------------
    // INTERNALS
    // ---------------------------------------------------------

    private <T> T post(
            String path,
            List<Map.Entry<String, String>> queryParams,
            Object body,
            Class<T> responseType,
            String errorMessage
    ) {
        try {
            String jsonBody = objectMapper.writeValueAsString(body);

            String responseBody = http.post(
                    AUTH_ENDPOINT + path,
                    queryParams,
                    jsonBody,
                    null
            );

            return objectMapper.readValue(responseBody, responseType);

        } catch (JacksonException e) {
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * Ensures the client has an access token.
     */
    private void requireAuthenticated() {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException(
                    "No access token present. " +
                            "Create an authenticated SupabaseClient first."
            );
        }
    }

    /**
     * Ensures a value is not null or blank.
     */
    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be null or blank"
            );
        }
    }
}
