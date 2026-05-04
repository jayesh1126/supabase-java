package io.github.jayesh1126.supabase.exception;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Exception thrown when a Supabase or PostgREST operation fails.
 *
 * <p>This is the unified exception type exposed by the SDK and represents
 * failures across three distinct categories:
 *
 * <ul>
 *   <li><b>HTTP errors</b> (4xx / 5xx): returned by Supabase/PostgREST.
 *       {@link #getStatusCode()} contains the HTTP status code.</li>
 *   <li><b>Network / I/O failures</b>: connectivity issues, timeouts, DNS errors.
 *       {@code statusCode = -1} and {@link #getCause()} is populated.</li>
 *   <li><b>Client-side errors</b>: such as deserialization failures.
 *       {@code statusCode = 0}.</li>
 * </ul>
 *
 * <p>When the server returns a structured PostgREST error body, additional
 * metadata is extracted and exposed via:
 * <ul>
 *   <li>{@link #getPostgrestCode()}</li>
 *   <li>{@link #getDetails()}</li>
 *   <li>{@link #getHint()}</li>
 * </ul>
 *
 * <p>Example PostgREST error response:
 * <pre>{@code
 * {
 *   "code":    "PGRST116",
 *   "message": "The result contains 0 rows",
 *   "details": null,
 *   "hint":    null
 * }
 * }</pre>
 *
 * <p>This exception is unchecked to avoid excessive boilerplate and is intended
 * to be handled at application boundaries (e.g. service layer, controller, or CLI entry point).
 */
public class SupabaseException extends RuntimeException {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int statusCode;
    private final String responseBody;

    // Parsed PostgREST error fields (nullable)
    private final String postgrestCode;
    private final String details;
    private final String hint;

    /**
     * Creates an exception representing an HTTP-level failure.
     *
     * <p>If the response body contains valid JSON, an attempt is made to extract
     * PostgREST-specific error fields. If parsing fails, the raw body is still preserved.
     *
     * @param statusCode   HTTP status code (4xx/5xx), or 0/-1 for client-side failures
     * @param message      Human-readable error message
     * @param responseBody Raw response body returned by the server (may be null)
     */
    public SupabaseException(int statusCode, String message, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;

        String parsedCode = null;
        String parsedDetails = null;
        String parsedHint = null;

        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode node = MAPPER.readTree(responseBody);
                parsedCode = textOrNull(node, "code");
                parsedDetails = textOrNull(node, "details");
                parsedHint = textOrNull(node, "hint");
            } catch (Exception ignored) {
                // Non-JSON or unexpected format — safe to ignore
            }
        }

        this.postgrestCode = parsedCode;
        this.details = parsedDetails;
        this.hint = parsedHint;
    }

    /**
     * Creates an exception wrapping a lower-level failure (e.g. network or I/O error).
     *
     * @param statusCode HTTP status code, or -1 for network failures
     * @param message    Human-readable message
     * @param cause      Underlying cause of the failure
     */
    public SupabaseException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = null;
        this.postgrestCode = null;
        this.details = null;
        this.hint = null;
    }

    /**
     * Returns the HTTP status code associated with this error.
     *
     * <ul>
     *   <li>{@code >= 100}: HTTP response from server</li>
     *   <li>{@code -1}: network or transport failure</li>
     *   <li>{@code 0}: client-side error (e.g. deserialization)</li>
     * </ul>
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the raw response body returned by the server, if available.
     *
     * <p>May be null for network failures or non-HTTP errors.
     */
    public String getResponseBody() {
        return responseBody;
    }

    /**
     * Returns the PostgREST error code (e.g. {@code "PGRST116"}), if present.
     */
    public String getPostgrestCode() {
        return postgrestCode;
    }

    /**
     * Returns additional error details provided by PostgREST, if present.
     */
    public String getDetails() {
        return details;
    }

    /**
     * Returns a hint from PostgREST that may help resolve the error.
     */
    public String getHint() {
        return hint;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        return (child != null && !child.isNull()) ? child.asText() : null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SupabaseException{")
                .append("statusCode=").append(statusCode)
                .append(", message='").append(getMessage()).append('\'');

        if (postgrestCode != null) {
            sb.append(", postgrestCode='").append(postgrestCode).append('\'');
        }
        if (details != null) {
            sb.append(", details='").append(details).append('\'');
        }
        if (hint != null) {
            sb.append(", hint='").append(hint).append('\'');
        }

        sb.append('}');
        return sb.toString();
    }
}