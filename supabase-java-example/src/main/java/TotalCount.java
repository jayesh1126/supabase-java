
/**
 * Wrapper for scalar RPC responses.
 * PostgREST returns scalar values as plain JSON — if deserialization
 * fails, change your Postgres function to return a JSON object instead:
 *   SELECT json_build_object('value', SUM(count)) FROM test_table;
 * And update this record to: record TotalCount(int value) {}
 */
public record TotalCount(int value) {}