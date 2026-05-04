package io.github.jayesh1126.supabase.postgrest;

import io.github.jayesh1126.supabase.core.SupabaseHttpClient;
import io.github.jayesh1126.supabase.exception.SupabaseException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

/**
 * Entry point for all PostgREST (database) operations.
 *
 * <p>This client exposes a fluent API for querying Postgres tables,
 * views, and RPC functions via Supabase's PostgREST layer.
 *
 * <p>Obtain via {@code SupabaseClient.postgrest()}.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * List<User> users = client.postgrest()
 *     .from("users")
 *     .eq("active", true)
 *     .selectList(User.class);
 * }</pre>
 *
 * <p>This class is stateless but depends on shared HTTP + serialization
 * components injected by {@code SupabaseClient}.
 */
public class PostgrestClient {

    private final PostgrestExecutor executor;

    /**
     * Package-private constructor.
     * Instantiated via {@link io.github.jayesh1126.supabase.SupabaseClient}.
     */
    public PostgrestClient(SupabaseHttpClient http, ObjectMapper objectMapper) {
        Objects.requireNonNull(http, "http must not be null");
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.executor = new PostgrestExecutor(http, objectMapper);
    }

    /**
     * Begins building a query against a table or view in the default schema.
     *
     * <p>Returns a new immutable {@link PostgrestQueryBuilder} instance.
     * You can chain filter and modifier methods before calling a terminal
     * method such as {@code selectList()}, {@code insert()}, or {@code delete()}.
     *
     * @param table Name of the PostgREST table or view (must not be null or blank)
     * @return a new query builder instance
     * @throws IllegalArgumentException if {@code table} is null or blank
     */
    public PostgrestQueryBuilder from(String table) {
        validateIdentifier(table, "table");
        return new PostgrestQueryBuilder(table, executor);
    }

    /**
     * Begins building a query against a table or view in a specific schema.
     *
     * <p>This allows querying non-default schemas:
     * <pre>{@code
     * client.postgrest()
     *     .from("private", "users")
     *     .selectList(User.class);
     * }</pre>
     *
     * @param schema Schema name (must not be null or blank)
     * @param table  Table or view name (must not be null or blank)
     * @return a new query builder instance with schema-qualified table
     * @throws IllegalArgumentException if schema or table is null or blank
     */
    public PostgrestQueryBuilder from(String schema, String table) {
        validateIdentifier(schema, "schema");
        validateIdentifier(table, "table");
        return new PostgrestQueryBuilder(schema + "." + table, executor);
    }

    /**
     * Calls a Postgres function and returns a single result.
     *
     * <p>Use for functions that return one row or a scalar value.
     * Sends {@code Accept: application/vnd.pgrst.object+json}.
     *
     * <p>Example:
     * <pre>
     *   User user = client.postgrest().rpc("get_user", Map.of("user_id", id), User.class);
     * </pre>
     *
     * @param functionName Name of the Postgres function
     * @param params Object whose fields map to function parameters; null for no-arg functions
     * @param returnType Class to deserialize the response into
     * @param <T> Return type
     * @throws SupabaseException on HTTP error or network failure
     */
    public <T> T rpc(String functionName, Object params, Class<T> returnType) {
        Objects.requireNonNull(functionName, "functionName must not be null");
        Objects.requireNonNull(returnType, "returnType must not be null");
        return executor.rpc(functionName, params, returnType);
    }

    /**
     * Calls a Postgres function and returns all result rows.
     *
     * <p>Use for functions declared as {@code RETURNS TABLE} or {@code RETURNS SETOF}.
     *
     * <p>Example:
     * <pre>
     *   List&lt;User&gt; users = client.postgrest().rpcList("search_users",
     *       Map.of("query", "alice"), User.class);
     * </pre>
     *
     * @param functionName Name of the Postgres function
     * @param params Object whose fields map to function parameters; null for no-arg functions
     * @param returnType Class to deserialize each returned row into
     * @param <T> Element type
     * @throws SupabaseException on HTTP error or network failure
     */
    public <T> List<T> rpcList(String functionName, Object params, Class<T> returnType) {
        Objects.requireNonNull(functionName, "functionName must not be null");
        Objects.requireNonNull(returnType, "returnType must not be null");
        return executor.rpcList(functionName, params, returnType);
    }

    // -------------------
    // INTERNALS
    // -------------------

    private void validateIdentifier(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");

        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        // Basic safety guard against SQL injection via PostgREST path abuse
        if (value.contains(" ") || value.contains(";")) {
            throw new IllegalArgumentException(name + " contains invalid characters");
        }
    }
}