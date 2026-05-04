package io.github.jayesh1126.supabase.postgrest;

import io.github.jayesh1126.supabase.core.SupabaseHttpClient;
import io.github.jayesh1126.supabase.exception.SupabaseException;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Internal execution engine for PostgREST operations.
 *
 * <p>This class is an internal implementation detail and is not part of the public API.
 * It is used exclusively by {@link PostgrestClient} and {@link PostgrestQueryBuilder}.
 *
 * <p>It translates query builder state into HTTP requests, applies PostgREST-specific
 * headers, and handles JSON serialization/deserialization.
 *
 * <p>All errors are normalized into {@link SupabaseException} to avoid leaking
 * HTTP, IO, or Jackson-specific exceptions.
 */
class PostgrestExecutor {

    private static final String PREFER_REPRESENTATION = "return=representation";
    private static final String ACCEPT_SINGLE = "application/vnd.pgrst.object+json";

    private final SupabaseHttpClient http;
    private final ObjectMapper mapper;

    /**
     * Package-private — instantiated by {@link PostgrestClient}.
     *
     * @param http   HTTP client that handles auth headers and URL construction
     * @param mapper Shared Jackson mapper for serialization and deserialization
     */
    PostgrestExecutor(SupabaseHttpClient http, ObjectMapper mapper) {
        this.http   = Objects.requireNonNull(http,"http must not be null");
        this.mapper = Objects.requireNonNull(mapper,"mapper must not be null");
    }

    // ===========================
    // SELECT
    // ===========================

    /**
     * Executes a SELECT and returns exactly one row as type {@code T}.
     *
     * <p>Sends {@code Accept: application/vnd.pgrst.object+json}, which instructs PostgREST to:
     * <ul>
     *   <li>Return a JSON object rather than a JSON array</li>
     *   <li>Respond with 406 if zero rows match — surfaced as {@link SupabaseException}</li>
     *   <li>Respond with 409 if more than one row matches — surfaced as {@link SupabaseException}</li>
     * </ul>
     *
     * <p>Called by {@link PostgrestQueryBuilder#selectSingle(Class)}.
     * The builder guarantees {@code single()} was set before this method is reached.
     *
     * @param query Builder carrying the path, filters, modifiers, and pagination
     * @param type  Class to deserialize the response JSON object into
     * @param <T>   Return type
     * @return The single matching row deserialized as {@code T}
     * @throws SupabaseException on HTTP error (406, 409, 5xx), network failure, or deserialization error
     */
    <T> T selectSingle(PostgrestQueryBuilder query, Class<T> type) {
        try {
            String json = http.get(query.getPath(), query.getQueryParams(),
                    Map.of("Accept", ACCEPT_SINGLE));
            return deserialize(json, type);
        } catch (SupabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new SupabaseException(0, "Failed selectSingle deserialization", e);
        }
    }

    /**
     * Executes a SELECT and returns all matching rows as {@code List<T>}.
     *
     * <p>Returns an empty list if no rows match — never returns null.
     * No {@code Accept} override is sent; PostgREST returns a JSON array by default.
     *
     * <p>Called by {@link PostgrestQueryBuilder#selectList(Class)}.
     *
     * @param query Builder carrying the path, filters, modifiers, and pagination
     * @param type  Class to deserialize each row into
     * @param <T>   Element type
     * @return List of matching rows, possibly empty; never null
     * @throws SupabaseException on HTTP error, network failure, or deserialization error
     */
    <T> List<T> selectMultiple(PostgrestQueryBuilder query, Class<T> type) {
        try {
            String json = http.get(query.getPath(), query.getQueryParams(), null);
            if (json == null || json.isBlank()) return Collections.emptyList();
            return deserializeList(json, type);
        } catch (SupabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new SupabaseException(0, "Failed selectMultiple deserialization", e);
        }
    }

    // ===========================
    // INSERT / UPSERT
    // ===========================

    /**
     * Executes an INSERT and returns the inserted rows.
     *
     * <p>Sends {@code Prefer: return=representation} so PostgREST echoes back the
     * inserted rows with any server-generated values (auto-generated IDs, default timestamps, etc.).
     * Without this header, PostgREST returns a 201 with an empty body.
     *
     * <p>The {@code body} may be a single object (produces one row) or a {@link List}
     * of objects (bulk insert). Jackson serializes both correctly.
     *
     * <p>Called by {@link PostgrestQueryBuilder#insert(Object, Class)}.
     *
     * @param query Builder carrying the table path and any query params (e.g. {@code on_conflict})
     * @param body  Single object or {@link List} of objects to insert
     * @param type  Class to deserialize each returned row into
     * @param <T>   Return element type
     * @return List of inserted rows with server-generated values populated
     * @throws SupabaseException on HTTP error (e.g. 409 unique violation), network failure,
     *                           serialization error, or deserialization error
     */
    <T> List<T> insert(PostgrestQueryBuilder query, Object body, Class<T> type) {
        try {
            String json = http.post(
                    query.getPath(),
                    query.getQueryParams(),
                    serialize(body),
                    Map.of("Prefer", PREFER_REPRESENTATION)
            );
            return deserializeList(json, type);
        } catch (SupabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new SupabaseException(0, "Insert failed", e);
        }
    }

    /**
     * Executes an upsert (INSERT with conflict resolution on primary key).
     *
     * <p>Sends {@code Prefer: resolution=merge-duplicates,return=representation}.
     * On primary key conflict, PostgREST updates the existing row with the provided values
     * rather than throwing a unique constraint error.
     *
     * <p>The {@code body} may be a single object or a {@link List} for bulk upsert.
     *
     * <p>Called by {@link PostgrestQueryBuilder#upsert(Object, Class)}.
     *
     * @param query Builder carrying the table path
     * @param body  Single object or {@link List} of objects to upsert
     * @param type  Class to deserialize each returned row into
     * @param <T>   Return element type
     * @return List of upserted rows
     * @throws SupabaseException on HTTP error, network failure, or serialization/deserialization error
     */
    <T> List<T> upsert(PostgrestQueryBuilder query, Object body, Class<T> type) {
        try {
            String json = http.post(
                    query.getPath(),
                    query.getQueryParams(),
                    serialize(body),
                    Map.of("Prefer", "resolution=merge-duplicates," + PREFER_REPRESENTATION)
            );
            return deserializeList(json, type);
        } catch (SupabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new SupabaseException(0, "Upsert failed", e);
        }
    }

    // ===========================
    // UPDATE
    // ===========================

    /**
     * Executes an UPDATE (PATCH) on rows matching the query filters.
     *
     * <p>Sends {@code Prefer: return=representation} to return the updated rows.
     * The builder enforces that at least one filter is set before this method is reached,
     * preventing accidental full-table updates.
     *
     * <p>Called by {@link PostgrestQueryBuilder#update(Object, Class)}.
     *
     * @param query Builder carrying the path and filters (at least one filter guaranteed)
     * @param body  Object containing the fields to update (only provided fields are changed)
     * @param type  Class to deserialize each updated row into
     * @param <T>   Return element type
     * @return List of updated rows
     * @throws SupabaseException on HTTP error, network failure, or serialization/deserialization error
     */
    <T> List<T> update(PostgrestQueryBuilder query, Object body, Class<T> type) {
        try {
            String json = http.patch(
                    query.getPath(),
                    query.getQueryParams(),
                    serialize(body),
                    Map.of("Prefer", PREFER_REPRESENTATION)
            );
            return deserializeList(json, type);
        } catch (SupabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new SupabaseException(0, "Update failed", e);
        }
    }

    // ===========================
    // DELETE
    // ===========================

    /**
     * Executes a DELETE on rows matching the query filters.
     *
     * <p>No response body is requested. The builder enforces that at least one filter
     * is set before this method is reached.
     *
     * <p>To retrieve the deleted rows, use {@link #deleteReturning(PostgrestQueryBuilder, Class)}.
     * Called by {@link PostgrestQueryBuilder#delete()}.
     *
     * @param query Builder carrying the path and filters (at least one filter guaranteed)
     * @throws SupabaseException on HTTP error or network failure
     */
    void delete(PostgrestQueryBuilder query) {
        try {
            http.delete(query.getPath(), query.getQueryParams(), null);
        } catch (SupabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new SupabaseException(0, "Delete failed", e);
        }
    }

    /**
     * Executes a DELETE on matching rows and returns the deleted rows.
     *
     * <p>Sends {@code Prefer: return=representation} so PostgREST returns the deleted
     * row data in the response body. The builder enforces that at least one filter is set.
     *
     * <p>Called by {@link PostgrestQueryBuilder#deleteReturning(Class)}.
     *
     * @param query Builder carrying the path and filters (at least one filter guaranteed)
     * @param type  Class to deserialize each deleted row into
     * @param <T>   Return element type
     * @return List of deleted rows
     * @throws SupabaseException on HTTP error, network failure, or deserialization error
     */
    <T> List<T> deleteReturning(PostgrestQueryBuilder query, Class<T> type) {
        try {
            String json = http.delete(
                    query.getPath(),
                    query.getQueryParams(),
                    Map.of("Prefer", PREFER_REPRESENTATION)
            );
            return deserializeList(json, type);
        } catch (SupabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new SupabaseException(0, "DeleteReturning failed", e);
        }
    }

    // ===========================
    // RPC
    // ===========================

    /**
     * Executes a PostgREST remote procedure call (RPC).
     *
     * <p>For scalar functions, returns a single JSON object.
     * For set-returning functions (SETOF), use {@link #rpcList(String, Object, Class)}.
     *
     * <p>Request body is serialized as JSON and passed to the Postgres function.
     */
    <T> T rpc(String functionName, Object params, Class<T> type) {
        validateFunctionName(functionName);

        try {
            String json = http.post(
                    "/rest/v1/rpc/" + functionName,
                    null,
                    serialize(params != null ? params : Collections.emptyMap()),
                    Map.of("Accept", ACCEPT_SINGLE)
            );
            return deserialize(json, type);
        } catch (SupabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new SupabaseException(0, "RPC failed: " + functionName, e);
        }
    }

    /**
     * Executes a PostgREST RPC expected to return a collection (SETOF result).
     *
     * <p>Use this method only for functions returning multiple rows.
     * For scalar functions, use {@link #rpc(String, Object, Class)} instead.
     */
    <T> List<T> rpcList(String functionName, Object params, Class<T> elementType) {
        validateFunctionName(functionName);

        try {
            String json = http.post(
                    "/rest/v1/rpc/" + functionName,
                    null,
                    serialize(params != null ? params : Collections.emptyMap()),
                    null
            );
            return deserializeList(json, elementType);
        } catch (SupabaseException e) {
            throw e;
        } catch (Exception e) {
            throw new SupabaseException(0, "RPC list failed: " + functionName, e);
        }
    }

    private void validateFunctionName(String functionName) {
        if (functionName == null || functionName.isBlank()) {
            throw new IllegalArgumentException("functionName must not be null or blank");
        }
    }

    // ===========================
    // PRIVATE HELPERS
    // ===========================

    private String serialize(Object body) {
        try {
            return mapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new SupabaseException(0, "JSON serialization failed", e);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new SupabaseException(0, "JSON deserialization failed", e);
        }
    }

    private <T> List<T> deserializeList(String json, Class<T> type) {
        try {
            return mapper.readValue(
                    json,
                    mapper.getTypeFactory().constructCollectionType(List.class, type)
            );
        } catch (Exception e) {
            throw new SupabaseException(0, "JSON list deserialization failed", e);
        }
    }
}