package io.github.jayesh1126.supabase;

import io.github.jayesh1126.supabase.auth.AuthClient;
import io.github.jayesh1126.supabase.core.SupabaseHttpClient;
import io.github.jayesh1126.supabase.postgrest.PostgrestClient;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * Entry point for the Supabase Java SDK.
 *
 * <p>This class acts as the composition root, wiring together all internal clients
 * (HTTP, serialization, and feature-specific APIs such as PostgREST).
 *
 * <p>Typical usage:
 * <pre>{@code
 * SupabaseClient client = new SupabaseClient(
 *     "https://xyz.supabase.co",
 *     "your-anon-key"
 * );
 *
 * List<User> users = client.postgrest()
 *     .from("users")
 *     .selectList(User.class);
 * }</pre>
 *
 * <p>Instances are thread-safe and intended to be reused across the application.
 *
 * <p>For authenticated operations, create a new instance with an access token:
 * <pre>{@code
 * SupabaseClient authClient = client.withAccessToken("user-access-token");
 * }</pre>
 * or obtain an access token via the AuthClient and then create an authenticated client
 */
public class SupabaseClient {

    private final PostgrestClient postgrest;
    private final AuthClient authClient;

    private final String accessToken;
    private final String supabaseUrl;
    private final String supabaseApiKey;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;


    /**
     * Creates a new unauthenticated Supabase client with the given URL and API key.
     * @param supabaseUrl Supabase project URL (e.g. {@code https://xyz.supabase.co})
     * @param supabaseApiKey Supabase anon or service-role key
     */
    public SupabaseClient(String supabaseUrl, String supabaseApiKey) {
        this(supabaseUrl, supabaseApiKey, null, defaultClient(), new ObjectMapper());
    }

    /**
     * Creates a new authenticated Supabase client with the given URL, API key, and access token.
     * @param supabaseUrl Supabase project URL (e.g. {@code https://xyz.supabase.co})
     * @param supabaseApiKey Supabase anon or service-role key
     * @param accessToken Supabase access token for authenticated requests (optional, can be null for unauthenticated clients)
     */
    public SupabaseClient(String supabaseUrl, String supabaseApiKey, String accessToken) {
        this(supabaseUrl, supabaseApiKey, accessToken, defaultClient(), new ObjectMapper());
    }

    /**
     * Internal constructor that allows full customization of HTTP httpClient and object mapper.
     * Used for reuse mainly.
     * @param supabaseUrl Supabase project URL (e.g. {@code https://xyz.supabase.co})
     * @param supabaseApiKey Supabase anon or service-role key
     * @param accessToken Supabase access token for authenticated requests (optional, can be null for unauthenticated clients)
     * @param httpClient Custom OkHttpClient instance (optional, if null a default httpClient with reasonable timeouts will be used)
     * @param objectMapper Custom ObjectMapper instance for JSON serialization (optional, if null a default ObjectMapper will be used)
     */
    private SupabaseClient(String supabaseUrl, String supabaseApiKey, String accessToken,
                           OkHttpClient httpClient, ObjectMapper objectMapper) {
        Objects.requireNonNull(supabaseUrl, "supabaseUrl must not be null");
        Objects.requireNonNull(supabaseApiKey, "supabaseApiKey must not be null");
        Objects.requireNonNull(httpClient, "httpClient must not be null");
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");

        HttpUrl parsed = HttpUrl.parse(supabaseUrl);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid Supabase URL: " + supabaseUrl);
        }

        SupabaseHttpClient http = new SupabaseHttpClient(
                supabaseUrl,
                supabaseApiKey,
                httpClient,
                accessToken
        );

        this.postgrest = new PostgrestClient(http, objectMapper);
        this.authClient = new AuthClient(http, objectMapper);

        this.accessToken = accessToken;
        this.supabaseUrl = supabaseUrl;
        this.supabaseApiKey = supabaseApiKey;

        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the PostgREST client used for database operations.
     *
     * <p>All queries start with {@code .from("table")} and are built fluently.
     */
    public PostgrestClient postgrest() {
        return postgrest;
    }

    /**
     * Returns the authentication client used for GoTrue operations.
     *
     * @return authentication client
     */
    public AuthClient auth() {
        return authClient;
    }

    /**
     * Returns a new SupabaseClient instance with the given access token.
     * This allows you to create an authenticated client after obtaining an access token via the AuthClient
     * @param accessToken Supabase access token for authenticated requests
     * @return a new SupabaseClient instance with the same URL, API key, HTTP client, and object mapper, but with the provided access token
     */
    public SupabaseClient withAccessToken(String accessToken) {
        return new SupabaseClient(supabaseUrl, supabaseApiKey, accessToken, httpClient, objectMapper);
    }


    /** Helper method to create a default OkHttpClient with reasonable timeouts.
     * Users can provide their own OkHttpClient via the three-arg constructor if they need custom configuration.
     */
    private static OkHttpClient defaultClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request request = chain.request().newBuilder()
                            .header("User-Agent", "supabase-java/0.1")
                            .build();
                    return chain.proceed(request);
                })
                .build();
    }
}