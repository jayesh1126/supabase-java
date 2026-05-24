package io.github.jayesh1126.supabase.auth;

import io.github.jayesh1126.supabase.auth.model.AuthResponse;
import io.github.jayesh1126.supabase.auth.model.User;
import io.github.jayesh1126.supabase.core.SupabaseHttpClient;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Client for Supabase Authentication API.
 *
 * <p>Provides methods for user sign-up, sign-in, token refresh, and sign-out.
 *
 * <p>Instances are thread-safe and can be reused across the application.
 *
 * <p>Typically obtained via {@link io.github.jayesh1126.supabase.SupabaseClient#auth()}.
 */
public class AuthClient {

    private final SupabaseHttpClient http;
    private final ObjectMapper objectMapper;
    private final String accessToken;
    private static final String AUTH_ENDPOINT = "/auth/v1";

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


    public AuthResponse signUpWithEmail(String email, String password) {
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(password, "password must not be null");

        try {
            Map<String, String> body = new HashMap<>();
            body.put("email", email);
            body.put("password", password);

            String jsonBody = objectMapper.writeValueAsString(body);

            String responseBody = http.post(AUTH_ENDPOINT + "/signup",
                    null,
                    jsonBody,
                    null
            );

            return objectMapper.readValue(responseBody, AuthResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign up with email", e);
        }
    }

    public AuthResponse signInWithEmail(String email, String password) {
        Objects.requireNonNull(email, "email must not be null");
        Objects.requireNonNull(password, "password must not be null");

        try {
            Map<String, String> body = new HashMap<>();
            body.put("email", email);
            body.put("password", password);

            String jsonBody = objectMapper.writeValueAsString(body);

            String responseBody = http.post(
                    AUTH_ENDPOINT + "/token",
                    List.of(Map.entry("grant_type", "password")),
                    jsonBody,
                    null
            );

            return objectMapper.readValue(responseBody, AuthResponse.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to sign in user", e);
        }
    }

    public AuthResponse refreshAccessToken(String refreshToken) {
        Objects.requireNonNull(refreshToken, "refreshToken must not be null");

        try {
            Map<String, String> body = new HashMap<>();
            body.put("refresh_token", refreshToken);

            String jsonBody = objectMapper.writeValueAsString(body);

            String responseBody = http.post(
                    AUTH_ENDPOINT + "/token",
                    List.of(Map.entry("grant_type", "refresh_token")),
                    jsonBody,
                    null
            );

            return objectMapper.readValue(responseBody, AuthResponse.class);

        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh token", e);
        }
    }

    public User getUser() {
        if (accessToken == null) {
            throw new IllegalStateException(
                    "No access token present. Create an authenticated SupabaseClient first."
            );
        }

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

    public void signOut() {
        if (accessToken == null) {
            throw new IllegalStateException(
                    "No access token present. Create an authenticated SupabaseClient first."
            );
        }

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
}
