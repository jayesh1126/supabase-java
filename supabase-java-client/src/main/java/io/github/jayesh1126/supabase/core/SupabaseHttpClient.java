package io.github.jayesh1126.supabase.core;

import okhttp3.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import io.github.jayesh1126.supabase.exception.SupabaseException;

/**
 * Low-level HTTP transport layer for Supabase.
 *
 * <p>This class is responsible for:
 * <ul>
 *   <li>Constructing request URLs using {@link HttpUrl.Builder}</li>
 *   <li>Attaching authentication headers to every request</li>
 *   <li>Executing HTTP calls via {@link OkHttpClient}</li>
 *   <li>Normalizing failures into {@link SupabaseException}</li>
 * </ul>
 *
 * <p><b>Design principles:</b>
 * <ul>
 *   <li>Stateless and thread-safe</li>
 *   <li>No knowledge of PostgREST semantics (delegated to higher layers)</li>
 *   <li>Minimal abstraction over OkHttp</li>
 * </ul>
 *
 * <p><b>Authentication:</b>
 * Every request includes:
 * <ul>
 *   <li>{@code apikey}</li>
 *   <li>{@code Authorization: Bearer <access-token-or-api-key>}</li>
 * </ul>
 *
 * <p><b>Query parameters:</b>
 * Represented as {@code List<Map.Entry<String,String>>} to allow duplicate keys,
 * which are required for PostgREST filters (e.g. {@code age=gt.18&age=lt.65}).
 *
 * <p><b>Error handling:</b>
 * <ul>
 *   <li>Non-2xx responses throw {@link SupabaseException}</li>
 *   <li>Network failures throw {@link SupabaseException} with {@code statusCode = -1}</li>
 * </ul>
 *
 * <p><b>Note:</b> This class returns raw response bodies. Higher-level clients
 * (e.g. PostgREST) are responsible for deserialization and domain logic.
 */
public class SupabaseHttpClient {
//    TODO: HEAD support and Response abstraction for status code + body (currently we just return raw body and throw on non-2xx)

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final HttpUrl baseUrl;
    private final String apiKey;
    private final String accessToken;

    /**
     * Creates a new HTTP client for Supabase.
     *
     * @param baseUrl Supabase project URL (e.g. {@code https://xyz.supabase.co})
     * @param apiKey  Supabase anon or service-role key
     * @param client  Preconfigured {@link OkHttpClient} instance
     *
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if {@code baseUrl} is not a valid URL
     */
    public SupabaseHttpClient(String baseUrl, String apiKey, OkHttpClient client, String accessToken) {
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        Objects.requireNonNull(apiKey,  "apiKey must not be null");
        Objects.requireNonNull(client,  "client must not be null");

        HttpUrl parsed = HttpUrl.parse(baseUrl);
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid baseUrl: " + baseUrl);
        }

        this.baseUrl = parsed;
        this.client  = client;
        this.apiKey  = apiKey;
        this.accessToken = accessToken;
    }

    /**
     * Executes a GET request.
     *
     * @param path        Relative path (e.g. {@code /rest/v1/users})
     * @param queryParams Query parameters (nullable; duplicate keys supported)
     * @param headers     Additional headers (nullable; may override defaults)
     * @return Raw response body (never null, may be empty)
     * @throws SupabaseException on HTTP or network failure
     */
    public String get(String path,
                      List<Map.Entry<String, String>> queryParams,
                      Map<String, String> headers) {
        return execute(buildRequest(path, queryParams, headers).get().build());
    }

    /**
     * Executes a POST request with a JSON body.
     *
     * @param path        Relative path
     * @param queryParams Query parameters (nullable)
     * @param jsonBody    JSON-encoded request body (must not be null)
     * @param headers     Additional headers (nullable)
     * @return Raw response body
     * @throws SupabaseException on HTTP or network failure
     */
    public String post(String path,
                       List<Map.Entry<String, String>> queryParams,
                       String jsonBody,
                       Map<String, String> headers) {

        Objects.requireNonNull(jsonBody, "jsonBody must not be null");

        RequestBody body = RequestBody.create(jsonBody, JSON);

        return execute(
                buildRequest(path, queryParams, headers)
                        .addHeader("Content-Type", "application/json")
                        .post(body)
                        .build()
        );
    }

    /**
     * Executes a PATCH request with a JSON body.
     *
     * <p><b>Warning:</b> PostgREST updates typically require filters to avoid
     * modifying all rows. This method does not enforce that constraint.
     *
     * @param path        Relative path
     * @param queryParams Query parameters (should include filters)
     * @param jsonBody    JSON-encoded request body (must not be null)
     * @param headers     Additional headers (nullable)
     * @return Raw response body
     * @throws SupabaseException on HTTP or network failure
     */
    public String patch(String path,
                        List<Map.Entry<String, String>> queryParams,
                        String jsonBody,
                        Map<String, String> headers) {

        Objects.requireNonNull(jsonBody, "jsonBody must not be null");

        RequestBody body = RequestBody.create(jsonBody, JSON);

        return execute(
                buildRequest(path, queryParams, headers)
                        .addHeader("Content-Type", "application/json")
                        .patch(body)
                        .build()
        );
    }

    /**
     * Executes a DELETE request.
     *
     * <p><b>Warning:</b> PostgREST deletes typically require filters to avoid
     * deleting all rows. This method does not enforce that constraint.
     *
     * @param path        Relative path
     * @param queryParams Query parameters (should include filters)
     * @param headers     Additional headers (nullable)
     * @return Raw response body (empty if no rows returned)
     * @throws SupabaseException on HTTP or network failure
     */
    public String delete(String path,
                         List<Map.Entry<String, String>> queryParams,
                         Map<String, String> headers) {

        return execute(
                buildRequest(path, queryParams, headers)
                        .delete()
                        .build()
        );
    }

    // -------------------
    // INTERNALS
    // -------------------

    /**
     * Executes the given request synchronously.
     *
     * @param request Fully constructed OkHttp request
     * @return Raw response body (never null)
     *
     * @throws SupabaseException if:
     * <ul>
     *   <li>The response status is non-2xx</li>
     *   <li>A network or I/O error occurs</li>
     * </ul>
     */
    private String execute(Request request) {
        try (Response response = client.newCall(request).execute()) {

            String responseBody = response.body() != null
                    ? response.body().string()
                    : "";

            if (!response.isSuccessful()) {
                throw new SupabaseException(
                        response.code(),
                        "HTTP " + response.code() + " from " + request.url(),
                        responseBody
                );
            }

            return responseBody;

        } catch (IOException e) {
            throw new SupabaseException(
                    -1,
                    "Network request failed: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Builds a request with authentication and custom headers applied.
     */
    private Request.Builder buildRequest(String path,
                                         List<Map.Entry<String, String>> queryParams,
                                         Map<String, String> headers) {

        HttpUrl url = buildUrl(path, queryParams);

        String bearerToken =
                accessToken != null && !accessToken.isBlank()
                        ? accessToken
                        : apiKey;

        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + bearerToken);

        if (headers != null) {
            headers.forEach(builder::addHeader);
        }

        return builder;
    }

    /**
     * Constructs a full URL from the base URL, path, and query parameters.
     *
     * <p>Path is split into segments to avoid issues with leading/trailing slashes.
     * Query parameters are encoded using OkHttp.
     *
     * @param path        Relative path (must not be null)
     * @param queryParams Query parameters (nullable)
     * @return Fully constructed {@link HttpUrl}
     */
    private HttpUrl buildUrl(String path,
                             List<Map.Entry<String, String>> queryParams) {

        Objects.requireNonNull(path, "path must not be null");

        HttpUrl.Builder builder = baseUrl.newBuilder();

        for (String segment : path.split("/")) {
            if (!segment.isEmpty()) {
                builder.addPathSegment(segment);
            }
        }

        if (queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams) {
                builder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }

        return builder.build();
    }
}