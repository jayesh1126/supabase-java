package io.github.jayesh1126.supabase.postgrest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import io.github.jayesh1126.supabase.exception.SupabaseException;

/**
 * Fluent, immutable builder for PostgREST queries.
 *
 * <p>Every method returns a new {@code PostgrestQueryBuilder} instance. The builder is
 * fully immutable, which means you can safely branch from a base query without side effects:
 * <pre>
 *   PostgrestQueryBuilder activeUsers = client.postgrest().from("users").eq("active", true);
 *   List&lt;User&gt; admins = activeUsers.eq("role", "admin").selectList(User.class);
 *   List&lt;User&gt; mods   = activeUsers.eq("role", "mod").selectList(User.class);
 * </pre>
 *
 * <p><b>Terminal methods</b> ({@link #selectList}, {@link #selectSingle}, {@link #insert},
 * {@link #update}, {@link #upsert}, {@link #delete}, {@link #deleteReturning}) fire the
 * HTTP request and return results. All other methods return a new builder for further chaining.
 *
 * <p><b>Encoding:</b> String filter values are quoted using PostgREST double-quote syntax
 * (e.g. {@code name=eq."John Doe"}) to handle spaces, commas, and special characters safely.
 * Numeric, boolean, date, and enum values are passed as plain strings.
 *
 * <p><b>Null filtering:</b> Use {@link #is(String, Object)} or {@link #isNull(String)} for
 * NULL checks. Passing {@code null} to other filter methods throws {@link IllegalArgumentException}.
 *
 * <p><b>Default projection:</b> If {@link #select(String)} is not called, PostgREST returns
 * all columns (equivalent to {@code SELECT *}).
 *
 * <p><b>Thread safety:</b> Individual builder instances are immutable and safe to share
 * across threads. Terminal calls are not idempotent — each one fires a network request.
 *
 * <p><b>Known limitations / TODOs:</b>
 * <ul>
 *   <li>TODO: {@link #or(String)} accepts raw PostgREST DSL with no validation or encoding.
 *       A typed {@code or(Consumer&lt;OrBuilder&gt;)} overload should be added for safety.</li>
 *   <li>TODO: {@link #validateColumn(String)} only checks for null/blank. A regex check
 *       against {@code ^[a-zA-Z_][a-zA-Z0-9_.]*$} should be added to reject invalid identifiers.</li>
 *   <li>TODO: No HEAD request support. PostgREST supports {@code HEAD /table} with
 *       {@code Prefer: count=exact} to retrieve total row count without body transfer.
 *       A {@code count()} terminal method and a {@code selectPaged()} method returning
 *       a {@code PagedResult<T>} wrapper with total count should be added.</li>
 *   <li>TODO: Missing PostgREST array/range operators: {@code cs} (contains), {@code cd}
 *       (contained by), {@code sl} (strictly left of), {@code sr} (strictly right of),
 *       {@code nxl} (no extend left), {@code nxr} (no extend right), {@code adj} (adjacent).</li>
 *   <li>TODO: Missing full-text search operators: {@code fts}, {@code plfts}, {@code phfts},
 *       {@code wfts}.</li>
 *   <li>TODO: {@code order()} does not support null ordering ({@code nullsfirst}/{@code nullslast}).
 *       PostgREST syntax: {@code order=col.asc.nullsfirst}.</li>
 *   <li>TODO: No async support. All terminal methods block the calling thread.
 *       A {@code CompletableFuture}-based API should be considered once
 *       {@link PostgrestExecutor} is extracted to an interface.</li>
 *   <li>TODO: {@link PostgrestExecutor} should be extracted to an interface to enable
 *       mocking in tests and future async transport implementations.</li>
 * </ul>
 */
public class PostgrestQueryBuilder {

    private final String table;
    private final PostgrestExecutor executor;
    private final List<Map.Entry<String, String>> filters;    // eq, gt, lt, neq, in, not, is, or
    private final List<Map.Entry<String, String>> modifiers;  // order
    private final List<Map.Entry<String, String>> pagination; // limit, offset
    private final String select;                              // projection (null = PostgREST default *)
    private final boolean single;

    /**
     * Valid PostgREST filter operators accepted by {@link #not(String, String, Object)}.
     */
    private static final java.util.Set<String> VALID_OPS = java.util.Set.of(
            "eq", "neq", "gt", "gte", "lt", "lte", "like", "ilike", "in", "is"
    );

    /**
     * Package-private constructor — called by {@link PostgrestClient#from(String)}.
     * Users never instantiate this directly.
     *
     * @param table Table or view name to query
     * @param executor Execution engine that translates this builder into HTTP calls
     * @throws NullPointerException if table is null
     * @throws IllegalArgumentException if table is blank
     */
    PostgrestQueryBuilder(String table, PostgrestExecutor executor) {
        Objects.requireNonNull(table,"table must not be null");
        if (table.isBlank()) throw new IllegalArgumentException("table must not be blank");
        this.table = table;
        this.executor = executor;
        this.filters = new ArrayList<>();
        this.modifiers = new ArrayList<>();
        this.pagination = new ArrayList<>();
        this.select = null;
        this.single = false;
    }

    /**
     * Private copy constructor used by all builder methods to produce new immutable instances.
     * All lists are defensively copied via {@link List#copyOf} to prevent mutation.
     */
    private PostgrestQueryBuilder(String table, PostgrestExecutor executor,
                                  List<Map.Entry<String, String>> filters,
                                  List<Map.Entry<String, String>> modifiers,
                                  List<Map.Entry<String, String>> pagination,
                                  String select, boolean single) {
        this.table = table;
        this.executor = executor;
        this.filters = List.copyOf(filters);
        this.modifiers = List.copyOf(modifiers);
        this.pagination = List.copyOf(pagination);
        this.select = select;
        this.single = single;
    }

    // ===========================
    // PROJECTION
    // ===========================

    /**
     * Specifies which columns to return, using a raw PostgREST projection string.
     *
     * <p>Accepts the full PostgREST column syntax. Examples:
     * <pre>
     *   .select("id,name,email")
     *   .select("id,orders(id,total)")   // embedded resource (foreign key join)
     *   .select("price::text")           // column type cast
     *   .select("*")                     // explicit wildcard (same as omitting select)
     * </pre>
     *
     * <p>If not called, PostgREST defaults to returning all columns.
     *
     * @param projection Raw PostgREST projection string; must not be null or blank
     * @throws NullPointerException     if projection is null
     * @throws IllegalArgumentException if projection is blank
     */
    public PostgrestQueryBuilder select(String projection) {
        Objects.requireNonNull(projection, "projection must not be null");
        if (projection.isBlank()) throw new IllegalArgumentException("projection must not be blank");
        return new PostgrestQueryBuilder(table, executor, filters, modifiers, pagination, projection, single);
    }

    /**
     * Convenience overload — joins the given column names with commas.
     *
     * <p>Equivalent to {@code select(String.join(",", columns))}.
     * Does not support PostgREST syntax extensions (casts, embedded resources).
     * For those, use {@link #select(String)} directly.
     *
     * <pre>
     *   .select(List.of("id", "name", "email"))
     *   // equivalent to: .select("id,name,email")
     * </pre>
     *
     * @param columns Non-empty list of column names
     * @throws NullPointerException     if columns is null
     * @throws IllegalArgumentException if columns is empty
     */
    public PostgrestQueryBuilder select(List<String> columns) {
        Objects.requireNonNull(columns, "columns must not be null");
        if (columns.isEmpty()) throw new IllegalArgumentException("columns must not be empty");
        return select(String.join(",", columns));
    }

    /**
     * Marks this query as expecting exactly one result row.
     *
     * <p>Must be paired with {@link #selectSingle(Class)} as the terminal call.
     * Sends {@code Accept: application/vnd.pgrst.object+json}, which instructs PostgREST to:
     * <ul>
     *   <li>Return a JSON object rather than a JSON array</li>
     *   <li>Respond with 406 if zero rows match the filters</li>
     *   <li>Respond with 409 if more than one row matches the filters</li>
     * </ul>
     *
     * <p>Both 406 and 409 are surfaced as {@link SupabaseException} with the corresponding
     * status code. This is a strict contract — use it only when your filters guarantee uniqueness
     * (e.g. filtering by primary key).
     *
     * <p>Calling {@link #selectList(Class)} after {@code single()} throws {@link IllegalStateException}.
     */
    public PostgrestQueryBuilder single() {
        return new PostgrestQueryBuilder(table, executor, filters, modifiers, pagination, select, true);
    }

    // ===========================
    // FILTERS
    // ===========================

    /**
     * Filters rows where {@code col} equals {@code val}.
     *
     * <p>Produces: {@code col=eq.val}
     *
     * <p>For NULL equality, use {@link #isNull(String)} or {@link #is(String, Object)} instead.
     * Passing {@code null} as {@code val} throws {@link IllegalArgumentException}.
     *
     * @param col Column name; must not be null or blank
     * @param val Filter value; must not be null
     * @throws NullPointerException     if val is null — use {@link #isNull(String)} for NULL checks
     * @throws IllegalArgumentException if col is null or blank, or val type is unsupported
     */
    public PostgrestQueryBuilder eq(String col, Object val) {
        return addFilter(col, "eq", val);
    }

    /**
     * Filters rows where {@code col} does not equal {@code val}.
     *
     * <p>Produces: {@code col=neq.val}
     *
     * @param col Column name; must not be null or blank
     * @param val Filter value; must not be null
     * @throws IllegalArgumentException if col is null/blank or val type is unsupported
     */
    public PostgrestQueryBuilder neq(String col, Object val) {
        return addFilter(col, "neq", val);
    }

    /**
     * Filters rows where {@code col} is strictly greater than {@code val}.
     *
     * <p>Produces: {@code col=gt.val}
     *
     * @param col Column name; must not be null or blank
     * @param val Filter value; must not be null
     */
    public PostgrestQueryBuilder gt(String col, Object val) {
        return addFilter(col, "gt", val);
    }

    /**
     * Filters rows where {@code col} is greater than or equal to {@code val}.
     *
     * <p>Produces: {@code col=gte.val}
     *
     * @param col Column name; must not be null or blank
     * @param val Filter value; must not be null
     */
    public PostgrestQueryBuilder gte(String col, Object val) {
        return addFilter(col, "gte", val);
    }

    /**
     * Filters rows where {@code col} is strictly less than {@code val}.
     *
     * <p>Produces: {@code col=lt.val}
     *
     * @param col Column name; must not be null or blank
     * @param val Filter value; must not be null
     */
    public PostgrestQueryBuilder lt(String col, Object val) {
        return addFilter(col, "lt", val);
    }

    /**
     * Filters rows where {@code col} is less than or equal to {@code val}.
     *
     * <p>Produces: {@code col=lte.val}
     *
     * @param col Column name; must not be null or blank
     * @param val Filter value; must not be null
     */
    public PostgrestQueryBuilder lte(String col, Object val) {
        return addFilter(col, "lte", val);
    }

    /**
     * Filters rows where {@code col} matches the SQL LIKE {@code pattern} (case-sensitive).
     *
     * <p>Use {@code %} as a wildcard character.
     * Produces: {@code col=like.pattern}
     *
     * <p>Example: {@code .like("name", "J%")} matches names starting with "J".
     *
     * @param col     Column name; must not be null or blank
     * @param pattern LIKE pattern; must not be null
     */
    public PostgrestQueryBuilder like(String col, String pattern) {
        return addFilter(col, "like", pattern);
    }

    /**
     * Filters rows where {@code col} matches the SQL ILIKE {@code pattern} (case-insensitive).
     *
     * <p>Use {@code %} as a wildcard character.
     * Produces: {@code col=ilike.pattern}
     *
     * <p>Example: {@code .ilike("name", "j%")} matches "John", "jane", "JAKE".
     *
     * @param col     Column name; must not be null or blank
     * @param pattern ILIKE pattern; must not be null
     */
    public PostgrestQueryBuilder ilike(String col, String pattern) {
        return addFilter(col, "ilike", pattern);
    }

    /**
     * Filters rows where {@code col} matches any value in {@code values}.
     *
     * <p>Produces: {@code col=in.(val1,val2,val3)}
     *
     * <p>String values are quoted individually. Numeric and other types are passed as plain strings.
     * PostgREST handles type coercion based on the target column's database type.
     *
     * <p>Example:
     * <pre>
     *   .in("status", List.of("active", "pending"))
     *   // → status=in.("active","pending")
     *
     *   .in("id", List.of(1, 2, 3))
     *   // → id=in.(1,2,3)
     * </pre>
     *
     * @param col    Column name; must not be null or blank
     * @param values Non-empty list of values to match; must not be null or empty
     * @throws IllegalArgumentException if values is empty
     */
    public PostgrestQueryBuilder in(String col, List<?> values) {
        Objects.requireNonNull(values, "values must not be null");
        if (values.isEmpty()) throw new IllegalArgumentException("in() values must not be empty");
        validateColumn(col);

        String joined = values.stream()
                .map(v -> v instanceof String s
                        ? "\"" + s.replace("\"", "\\\"") + "\""  // strings need quotes in in()
                        : encodeValue(v))                          // numbers/booleans do not
                .collect(Collectors.joining(","));

        // Bypass addFilter — the joined string is already fully encoded
        List<Map.Entry<String, String>> newFilters = new ArrayList<>(filters);
        newFilters.add(Map.entry(col, "in.(" + joined + ")"));
        return new PostgrestQueryBuilder(table, executor, newFilters, modifiers, pagination, select, single);
    }

    /**
     * Negates any PostgREST filter operator.
     *
     * <p>Produces: {@code col=not.op.val}
     *
     * <p>Examples:
     * <pre>
     *   .not("status", "eq", "deleted")    // status=not.eq."deleted"
     *   .not("score", "gt", 100)           // score=not.gt.100
     * </pre>
     *
     * @param col Column name; must not be null or blank
     * @param op  PostgREST operator to negate; must be one of:
     *            {@code eq, neq, gt, gte, lt, lte, like, ilike, in, is}
     * @param val Value for the negated filter; must not be null
     * @throws IllegalArgumentException if op is not a recognised PostgREST operator
     */
    public PostgrestQueryBuilder not(String col, String op, Object val) {
        validateColumn(col);
        Objects.requireNonNull(op, "op must not be null");
        if (!VALID_OPS.contains(op.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Invalid operator for not(): '" + op + "'. Valid operators: " + VALID_OPS
            );
        }
        List<Map.Entry<String, String>> newFilters = new ArrayList<>(filters);
        newFilters.add(Map.entry(col, "not." + op.toLowerCase() + "." + encodeValue(val)));
        return new PostgrestQueryBuilder(table, executor, newFilters, modifiers, pagination, select, single);
    }

    /**
     * Adds an OR filter combining multiple conditions using raw PostgREST DSL.
     *
     * <p>Pass the comma-separated expression <b>without outer parentheses</b> —
     * this method wraps it automatically.
     * Produces: {@code or=(expression)}
     *
     * <p>Example:
     * <pre>
     *   .or("age.gt.18,status.eq.active")
     *   // → ?or=(age.gt.18,status.eq.active)
     * </pre>
     *
     * <p><b>Warning:</b> This method accepts a raw PostgREST expression string with no
     * validation or value encoding. It is an escape hatch for complex conditions not
     * yet expressible through the typed API. Prefer typed filter methods where possible.
     *
     * <p>TODO: Add a typed {@code or(Consumer&lt;OrBuilder&gt;)} overload that builds
     * the expression using the same encoding as the individual filter methods.
     *
     * @param expression Raw PostgREST OR expression without outer parentheses; must not be null or blank
     * @throws NullPointerException     if expression is null
     * @throws IllegalArgumentException if expression is blank
     */
    public PostgrestQueryBuilder or(String expression) {
        Objects.requireNonNull(expression, "or expression must not be null");
        if (expression.isBlank()) throw new IllegalArgumentException("or expression must not be blank");
        List<Map.Entry<String, String>> newFilters = new ArrayList<>(filters);
        newFilters.add(Map.entry("or", "(" + expression + ")"));
        return new PostgrestQueryBuilder(table, executor, newFilters, modifiers, pagination, select, single);
    }

    /**
     * Filters rows using PostgREST's strict {@code IS} operator.
     *
     * <p>Use this for NULL checks and strict boolean comparisons. The standard equality
     * operator ({@link #eq}) cannot be used for NULL — {@code col=eq.null} matches the
     * literal string {@code "null"}, not SQL NULL.
     *
     * <p>Produces: {@code col=is.null}, {@code col=is.true}, or {@code col=is.false}
     *
     * <p>Examples:
     * <pre>
     *   .is("deleted_at", null)  // deleted_at=is.null  (SQL IS NULL)
     *   .is("active", true)      // active=is.true      (strict boolean check)
     *   .is("active", false)     // active=is.false
     * </pre>
     *
     * @param col   Column name; must not be null or blank
     * @param value Must be {@code null}, {@code Boolean.TRUE}, or {@code Boolean.FALSE}
     * @throws IllegalArgumentException if value is not null or a Boolean
     */
    public PostgrestQueryBuilder is(String col, Object value) {
        validateColumn(col);
        String encoded;
        if (value == null)                 encoded = "null";
        else if (value instanceof Boolean) encoded = value.toString();
        else throw new IllegalArgumentException(
                    "is() only accepts null, true, or false. Got: " + value.getClass().getName()
            );
        List<Map.Entry<String, String>> newFilters = new ArrayList<>(filters);
        newFilters.add(Map.entry(col, "is." + encoded));
        return new PostgrestQueryBuilder(table, executor, newFilters, modifiers, pagination, select, single);
    }

    /**
     * Shorthand for {@code .is(col, null)}.
     *
     * <p>Filters rows where {@code col} IS NULL.
     * Produces: {@code col=is.null}
     *
     * <p>Equivalent SQL: {@code WHERE col IS NULL}
     *
     * @param col Column name; must not be null or blank
     */
    public PostgrestQueryBuilder isNull(String col) {
        return is(col, null);
    }

    // ===========================
    // MODIFIERS
    // ===========================

    /**
     * Orders results by a column.
     *
     * <p>Multiple calls to {@code order()} are additive — each adds a secondary sort key.
     * Produces: {@code order=col.direction}
     *
     * <p>Examples:
     * <pre>
     *   .order("created_at", "desc")
     *   .order("name", "asc")
     *   // → order=created_at.desc&order=name.asc
     * </pre>
     *
     * <p>TODO: Add nullsfirst/nullslast support.
     * PostgREST syntax: {@code order=col.asc.nullsfirst}
     *
     * @param col       Column name to order by; must not be null or blank
     * @param direction Sort direction: {@code "asc"} or {@code "desc"} (case-insensitive)
     * @throws IllegalArgumentException if direction is not {@code "asc"} or {@code "desc"}
     */
    public PostgrestQueryBuilder order(String col, String direction) {
        validateColumn(col);
        String dir = direction.toLowerCase();
        if (!dir.equals("asc") && !dir.equals("desc"))
            throw new IllegalArgumentException(
                    "Invalid order direction: '" + direction + "'. Must be 'asc' or 'desc'."
            );
        return addModifier("order", col + "." + dir);
    }

    // ===========================
    // PAGINATION
    // ===========================

    /**
     * Limits the number of rows returned.
     *
     * <p>If called multiple times, the last call wins (previous value is replaced).
     * Produces: {@code limit=n}
     *
     * <p>For combined offset + limit, prefer {@link #range(int, int)}.
     *
     * @param limit Maximum number of rows; must be >= 1
     * @throws IllegalArgumentException if limit is less than 1
     */
    public PostgrestQueryBuilder limit(int limit) {
        if (limit < 1) throw new IllegalArgumentException("limit must be >= 1, got: " + limit);
        return replacePagination("limit", String.valueOf(limit));
    }

    /**
     * Skips the first {@code offset} rows before returning results.
     *
     * <p>If called multiple times, the last call wins (previous value is replaced).
     * Produces: {@code offset=n}
     *
     * <p>For combined offset + limit, prefer {@link #range(int, int)}.
     *
     * @param offset Number of rows to skip; must be >= 0
     * @throws IllegalArgumentException if offset is negative
     */
    public PostgrestQueryBuilder offset(int offset) {
        if (offset < 0) throw new IllegalArgumentException("offset must be >= 0, got: " + offset);
        return replacePagination("offset", String.valueOf(offset));
    }

    /**
     * Returns rows between index {@code from} and {@code to} inclusive.
     *
     * <p>Implemented as {@code offset=from&limit=(to-from+1)}.
     * Replaces any existing {@code limit} and {@code offset} values set by prior
     * {@link #limit} or {@link #offset} calls.
     *
     * <p>Examples:
     * <pre>
     *   .range(0, 9)   // returns 10 rows (indices 0–9), offset=0&limit=10
     *   .range(10, 19) // returns the next 10 rows, offset=10&limit=10
     * </pre>
     *
     * @param from Start index, inclusive (>= 0)
     * @param to   End index, inclusive (must be >= from)
     * @throws IllegalArgumentException if from is negative or to is less than from
     */
    public PostgrestQueryBuilder range(int from, int to) {
        if (from < 0) throw new IllegalArgumentException("range 'from' must be >= 0, got: " + from);
        if (to < from) throw new IllegalArgumentException("range 'to' must be >= 'from', got: to=" + to + " from=" + from);
        List<Map.Entry<String, String>> newPagination = new ArrayList<>();
        newPagination.add(Map.entry("offset", String.valueOf(from)));
        newPagination.add(Map.entry("limit",  String.valueOf(to - from + 1)));
        return new PostgrestQueryBuilder(table, executor, filters, modifiers, newPagination, select, single);
    }

    // ===========================
    // TERMINAL METHODS
    // ===========================

    /**
     * Executes a SELECT and returns exactly one result row.
     *
     * <p>Requires {@link #single()} to have been called on the builder first. This
     * requirement enforces an explicit declaration of intent — if your filters don't
     * guarantee a unique row, use {@link #selectList(Class)} instead.
     *
     * <p>Sends {@code Accept: application/vnd.pgrst.object+json}. PostgREST will respond with:
     * <ul>
     *   <li>406 — zero rows matched your filters (surfaced as {@link SupabaseException})</li>
     *   <li>409 — more than one row matched your filters (surfaced as {@link SupabaseException})</li>
     * </ul>
     *
     * @param type Class to deserialize the response JSON object into; must not be null
     * @param <T>  Return type
     * @return The single matching row deserialized as {@code T}
     * @throws IllegalStateException if {@link #single()} was not called on this builder
     * @throws SupabaseException      on HTTP errors (406, 409, 5xx) or network failures
     */
    public <T> T selectSingle(Class<T> type) {
        if (!single) {
            throw new IllegalStateException(
                    "selectSingle() requires .single() on the builder. " +
                            "Call .single().selectSingle(), or use .selectList() for multiple rows."
            );
        }
        Objects.requireNonNull(type, "type must not be null");
        return executor.selectSingle(this, type);
    }

    /**
     * Executes a SELECT and returns all matching rows.
     *
     * <p>Returns an empty list if no rows match — never returns null.
     *
     * <p>For server-side row limits, chain {@link #limit(int)} before calling this method.
     *
     * @param type Class to deserialize each row into; must not be null
     * @param <T>  Element type of the returned list
     * @return List of matching rows, possibly empty; never null
     * @throws IllegalStateException if {@link #single()} was set — use {@link #selectSingle} instead
     * @throws SupabaseException      on HTTP errors or network failures
     */
    public <T> List<T> selectList(Class<T> type) {
        if (single) {
            throw new IllegalStateException(
                    "single() was set on this builder. Use .selectSingle() instead of .selectList()."
            );
        }
        Objects.requireNonNull(type, "type must not be null");
        return executor.selectMultiple(this, type);
    }

    /**
     * Executes an INSERT and returns the inserted rows.
     *
     * <p>Accepts either a single object or a {@link List} of objects for bulk insert.
     * Sends {@code Prefer: return=representation} so PostgREST returns the inserted rows
     * with any server-generated values (e.g. auto-generated IDs, default timestamps).
     *
     * <p>Example:
     * <pre>
     *   User created = client.postgrest()
     *       .from("users")
     *       .insert(newUser, User.class)
     *       .get(0);
     * </pre>
     *
     * @param body       Single object or {@link List} of objects to insert; must not be null
     * @param returnType Class to deserialize each returned row into; must not be null
     * @param <T>        Return element type
     * @return List of inserted rows with server-generated values populated
     * @throws SupabaseException on HTTP errors (e.g. 409 unique constraint) or network failures
     */
    public <T> List<T> insert(Object body, Class<T> returnType) {
        Objects.requireNonNull(body, "insert body must not be null");
        Objects.requireNonNull(returnType, "returnType must not be null");
        return executor.insert(this, body, returnType);
    }

    /**
     * Executes an upsert (INSERT with conflict resolution on primary key).
     *
     * <p>On primary key conflict, existing rows are updated with the provided field values.
     * Sends {@code Prefer: resolution=merge-duplicates,return=representation}.
     *
     * <p>Accepts either a single object or a {@link List} of objects.
     *
     * @param body       Single object or {@link List} of objects to upsert; must not be null
     * @param returnType Class to deserialize each returned row into; must not be null
     * @param <T>        Return element type
     * @return List of upserted rows with server-generated values populated
     * @throws SupabaseException on HTTP errors or network failures
     */
    public <T> List<T> upsert(Object body, Class<T> returnType) {
        Objects.requireNonNull(body, "upsert body must not be null");
        Objects.requireNonNull(returnType, "returnType must not be null");
        return executor.upsert(this, body, returnType);
    }

    /**
     * Executes an UPDATE on all rows matching the current filters.
     *
     * <p>At least one filter <b>must</b> be set before calling this method.
     * This is a safety guard against accidental full-table updates.
     * Sends {@code Prefer: return=representation} to return updated rows.
     *
     * <p>Example:
     * <pre>
     *   List&lt;User&gt; updated = client.postgrest()
     *       .from("users")
     *       .eq("id", 42)
     *       .update(Map.of("name", "Alice"), User.class);
     * </pre>
     *
     * @param body       Object containing the fields to update; must not be null
     * @param returnType Class to deserialize each updated row into; must not be null
     * @param <T>        Return element type
     * @return List of updated rows
     * @throws IllegalStateException if no filters have been set on this builder
     * @throws SupabaseException      on HTTP errors or network failures
     */
    public <T> List<T> update(Object body, Class<T> returnType) {
        requireFilters("update");
        Objects.requireNonNull(body, "update body must not be null");
        Objects.requireNonNull(returnType, "returnType must not be null");
        return executor.update(this, body, returnType);
    }

    /**
     * Executes a DELETE on all rows matching the current filters.
     *
     * <p>At least one filter <b>must</b> be set before calling this method.
     * This is a safety guard against accidental full-table deletes.
     *
     * <p>To retrieve the deleted rows, use {@link #deleteReturning(Class)} instead.
     *
     * @throws IllegalStateException if no filters have been set on this builder
     * @throws SupabaseException      on HTTP errors or network failures
     */
    public void delete() {
        requireFilters("delete");
        executor.delete(this);
    }

    /**
     * Executes a DELETE on matching rows and returns the deleted rows.
     *
     * <p>At least one filter <b>must</b> be set before calling this method.
     * Sends {@code Prefer: return=representation} to retrieve deleted row data.
     *
     * <p>Use this when you need to know what was deleted (e.g. for audit logging,
     * undo operations, or returning IDs to the caller).
     *
     * @param returnType Class to deserialize each deleted row into; must not be null
     * @param <T>        Return element type
     * @return List of deleted rows
     * @throws IllegalStateException if no filters have been set on this builder
     * @throws SupabaseException      on HTTP errors or network failures
     */
    public <T> List<T> deleteReturning(Class<T> returnType) {
        requireFilters("deleteReturning");
        Objects.requireNonNull(returnType, "returnType must not be null");
        return executor.deleteReturning(this, returnType);
    }

    // ===========================
    // PACKAGE-PRIVATE ACCESSORS
    // ===========================

    /**
     * Returns the PostgREST REST path for this builder's table.
     * Used by {@link PostgrestExecutor} to construct the request URL.
     */
    String getPath() {
        return "/rest/v1/" + table;
    }

    /**
     * Returns all query parameters for this builder in the correct PostgREST order:
     * projection, filters, modifiers, pagination.
     *
     * <p>Used by {@link PostgrestExecutor} when building the HTTP request.
     * Returns a mutable list safe to modify — it is a fresh copy each time.
     */
    List<Map.Entry<String, String>> getQueryParams() {
        List<Map.Entry<String, String>> params = new ArrayList<>();
        if (select != null) params.add(Map.entry("select", select));
        params.addAll(filters);
        params.addAll(modifiers);
        params.addAll(pagination);
        return params;
    }

    // ===========================
    // PRIVATE HELPERS
    // ===========================

    /**
     * Adds a filter to the filters list using the standard PostgREST operator syntax.
     * Encodes the value via {@link #encodeValue(Object)}.
     *
     * @throws NullPointerException     if value is null (use {@link #is} for NULL checks)
     * @throws IllegalArgumentException if col is invalid or value type is unsupported
     */
    private PostgrestQueryBuilder addFilter(String col, String op, Object value) {
        validateColumn(col);
        Objects.requireNonNull(value,
                "filter value for column '" + col + "' must not be null — use .is(col, null) for NULL checks");
        List<Map.Entry<String, String>> newFilters = new ArrayList<>(filters);
        newFilters.add(Map.entry(col, op + "." + encodeValue(value)));
        return new PostgrestQueryBuilder(table, executor, newFilters, modifiers, pagination, select, single);
    }

    /**
     * Appends a modifier (e.g. {@code order}) to the modifiers list.
     * Multiple modifiers with the same key are allowed (e.g. multi-column ordering).
     */
    private PostgrestQueryBuilder addModifier(String key, String value) {
        List<Map.Entry<String, String>> newModifiers = new ArrayList<>(modifiers);
        newModifiers.add(Map.entry(key, value));
        return new PostgrestQueryBuilder(table, executor, filters, newModifiers, pagination, select, single);
    }

    /**
     * Replaces an existing pagination key ({@code limit} or {@code offset}) if present,
     * then appends the new value. Prevents duplicate pagination params when chaining
     * {@link #limit}, {@link #offset}, and {@link #range}.
     */
    private PostgrestQueryBuilder replacePagination(String key, String value) {
        List<Map.Entry<String, String>> newPagination = pagination.stream()
                .filter(e -> !e.getKey().equals(key))
                .collect(Collectors.toCollection(ArrayList::new));
        newPagination.add(Map.entry(key, value));
        return new PostgrestQueryBuilder(table, executor, filters, modifiers, newPagination, select, single);
    }

    /**
     * Validates that a column name is non-null and non-blank.
     *
     * <p>TODO: Strengthen with regex validation against {@code ^[a-zA-Z_][a-zA-Z0-9_.]*$}
     * to reject identifiers containing SQL-special characters (semicolons, quotes, etc.).
     * Currently only null/blank is caught; invalid characters produce a PostgREST HTTP error
     * rather than a clear local exception.
     */
    private void validateColumn(String col) {
        if (col == null || col.isBlank()) {
            throw new IllegalArgumentException("Column name must not be null or blank");
        }
    }

    /**
     * Asserts that at least one filter has been set.
     * Called by mutating terminal methods ({@link #update}, {@link #delete},
     * {@link #deleteReturning}) to prevent accidental full-table operations.
     *
     * @param operation Name of the calling operation, used in the exception message
     * @throws IllegalStateException if no filters are set
     */
    private void requireFilters(String operation) {
        if (filters.isEmpty()) {
            throw new IllegalStateException(
                    operation + "() requires at least one filter. " +
                            "Call .eq(), .gt(), .is(), etc. before calling " + operation + "()."
            );
        }
    }

    /**
     * Encodes a filter value into a PostgREST-safe string representation.
     *
     * <p>Encoding rules:
     * <ul>
     *   <li>{@link String} — wrapped in double quotes: {@code "value"}. Internal double
     *       quotes are escaped as {@code \"}. OkHttp percent-encodes the quotes in the URL;
     *       PostgREST decodes them. This handles spaces, commas, and operator-like strings safely.</li>
     *   <li>{@link Boolean} — {@code true} or {@code false}</li>
     *   <li>{@link Number} (Integer, Long, Double, BigDecimal, etc.) — plain decimal string</li>
     *   <li>{@link Instant} — ISO-8601 UTC string, e.g. {@code 2024-01-15T10:30:00Z}</li>
     *   <li>{@link LocalDate} — ISO-8601 date string, e.g. {@code 2024-01-15}</li>
     *   <li>{@link Enum} — enum constant name via {@link Enum#name()}</li>
     * </ul>
     *
     * <p>Null is not accepted — callers must guard against null before calling this method.
     *
     * <p>TODO: Consider extracting this to a package-private {@code PostgrestValueEncoder}
     * utility class once OrBuilder is added, to avoid duplicating the logic.
     *
     * @throws IllegalArgumentException for unsupported types
     */
    private String encodeValue(Object value) {
        if (value instanceof String s)   return s;
        if (value instanceof Boolean)    return value.toString();
        if (value instanceof Number)     return value.toString();
        if (value instanceof Instant)    return ((Instant) value).toString();
        if (value instanceof LocalDate)  return ((LocalDate) value).toString();
        if (value instanceof Enum<?>)    return ((Enum<?>) value).name();
        throw new IllegalArgumentException(
                "Unsupported filter value type: " + value.getClass().getName() +
                        ". Supported types: String, Number, Boolean, Instant, LocalDate, Enum."
        );
    }
}