package io.github.jayesh1126.supabase.auth;

import io.github.jayesh1126.supabase.auth.model.AuthResponse;
import io.github.jayesh1126.supabase.auth.model.User;
import io.github.jayesh1126.supabase.core.SupabaseHttpClient;
import tools.jackson.databind.ObjectMapper;

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

    /**
    * Package-private constructor.
    * Instantiated via {@link io.github.jayesh1126.supabase.SupabaseClient}.
    */
    public AuthClient(SupabaseHttpClient http, ObjectMapper objectMapper) {
        Objects.requireNonNull(http, "http must not be null");
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");

        this.http = http;
        this.objectMapper = objectMapper;
    }


    public AuthResponse signUpWithEmail(String email, String password) {

    }

    public AuthResponse signInWithEmail(String email, String password) {

    }

    public User getUser(String accessToken) {

    }

    public AuthResponse refreshAccessToken(String refreshToken) {

    }

    public void signOut(String accessToken) {

    }
}
