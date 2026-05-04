package io.github.jayesh1126.supabase;

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
 * <p>Advanced users may provide a custom {@link OkHttpClient} to configure
 * timeouts, logging, retries, or interceptors.
 */
public class SupabaseClient {

    private final PostgrestClient postgrest;

    /**
     * Creates a Supabase client with default HTTP configuration.
     *
     * @param supabaseUrl Base URL of your Supabase project (e.g. https://xyz.supabase.co)
     * @param supabaseApiKey Supabase API key (anon or service role)
     * @throws IllegalArgumentException if the URL is invalid
     */
    public SupabaseClient(String supabaseUrl, String supabaseApiKey) {
        this(supabaseUrl, supabaseApiKey, defaultClient(), null);
    }

    /**
     * Creates a Supabase client with a custom HTTP client and ObjectMapper.
     *
     * @param supabaseUrl Base URL of your Supabase project
     * @param supabaseApiKey Supabase API key
     * @param client Preconfigured OkHttpClient instance
     * @param objectMapper Custom ObjectMapper (optional, defaults to a new instance if null)
     */
    public SupabaseClient(String supabaseUrl, String supabaseApiKey, OkHttpClient client, ObjectMapper objectMapper) {
        Objects.requireNonNull(supabaseUrl, "supabaseUrl must not be null");
        Objects.requireNonNull(supabaseApiKey, "supabaseApiKey must not be null");
        Objects.requireNonNull(client, "client must not be null");

        HttpUrl parsed = HttpUrl.parse(supabaseUrl);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid Supabase URL: " + supabaseUrl);
        }

        ObjectMapper mapper = (objectMapper != null) ? objectMapper : new ObjectMapper();

        SupabaseHttpClient http = new SupabaseHttpClient(
                supabaseUrl,
                supabaseApiKey,
                client
        );

        this.postgrest = new PostgrestClient(http, mapper);
    }

    /**
     * Returns the PostgREST client used for database operations.
     *
     * <p>All queries start with {@code .from("table")} and are built fluently.
     */
    public PostgrestClient postgrest() {
        return postgrest;
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