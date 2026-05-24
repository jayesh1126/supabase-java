# supabase-java

> An unofficial Java client for Supabase, designed to mirror the ergonomics of the official JavaScript SDK.

![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)
![Java](https://img.shields.io/badge/Java-17%2B-blue)

---

## Why

There is currently no official Java SDK for Supabase.

`supabase-java` provides a **type-safe, fluent, and idiomatic Java API** for interacting with Supabase — removing the need to deal with HTTP, headers, or JSON manually.

It is designed for backend engineers building on the JVM who want a clean, production-ready developer experience similar to `supabase-js`.

---

## Status

| Module | Status        |
|---|---------------|
| PostgREST (Database) | ✅ Implemented |
| Auth (GoTrue) | ✅ Implemented |
| Storage | 🔜 Planned    |
| Edge Functions | 🔜 Planned    |
| Realtime | 🔜 Planned    |

## Quickstart

```java
SupabaseClient client = new SupabaseClient("https://xyz.supabase.co", "your-anon-key");

List<User> users = client.postgrest()
    .from("users")
    .eq("active", true)
    .order("created_at", "desc")
    .limit(10)
    .selectList(User.class);
```

**example-service/QUICKSTART.md** has a more detailed quickstart guide with step-by-step instructions and troubleshooting tips.

---

## Requirements

- Java 17 or higher
- Maven 3.6+

---

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.jayesh1126</groupId>
    <artifactId>supabase-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

> **Note:** The library is not yet published to Maven Central. Clone the repository and run `mvn install` to install it locally.

---

## Getting Started

### Create a client

Instantiate `SupabaseClient` once and reuse it across your application. It is thread-safe.

```java
// With anon key (public access)
SupabaseClient client = new SupabaseClient(
    "https://xyz.supabase.co",
    "your-anon-key"
);

// With access token (authenticated user)
SupabaseClient client = new SupabaseClient(
        "https://xyz.supabase.co",
        "your-anon-key",
        "user-access-token"
);
```
---

## Auth — Authentication

All auth operations start from `client.auth()`.

### Sign up

```java
AuthResponse response = client.auth().signUp(
        "user@example.com",
        "password123"
);

String accessToken = response.getAccessToken();
String refreshToken = response.getRefreshToken();
```

## Sign in

```java
AuthResponse response = client.auth().signIn(
        "user@example.com",
        "password123"
);
```

## Sign out

```java
client.auth().signOut("access-token");
```

## Refresh token

```java
AuthResponse response = client.auth().refreshToken("refresh-token");
```
---

## PostgREST — Database

All database operations start from `client.postgrest().from("table_name")`.

### Define your model

```java
public class User {
    public String id;
    public String name;
    public String email;
    public boolean active;
    public String createdAt;
}
```

Jackson is used for deserialization. Your fields must match the column names returned by PostgREST, or use `@JsonProperty` to map them.

---

### SELECT

#### Fetch multiple rows

```java
List<User> users = client.postgrest()
    .from("users")
    .selectList(User.class);
```

#### Fetch specific columns

```java
List<User> users = client.postgrest()
    .from("users")
    .select("id,name,email")
    .selectList(User.class);
```

#### Fetch a single row

Use `.single()` when your filters are guaranteed to match exactly one row (e.g. filtering by primary key). PostgREST will return a `406` if zero rows match and a `409` if more than one row matches — both surfaced as `SupabaseException`.

```java
User user = client.postgrest()
    .from("users")
    .eq("id", "abc-123")
    .single()
    .selectSingle(User.class);
```

---

### Filters

All filter methods are immutable and chainable. Multiple filters are combined with AND.

```java
// Equality
.eq("status", "active")

// Inequality
.neq("role", "banned")

// Comparisons
.gt("age", 18)
.gte("score", 100)
.lt("price", 50)
.lte("rank", 10)

// Pattern matching
.like("name", "J%")        // case-sensitive
.ilike("email", "%@gmail%") // case-insensitive

// IN list
.in("status", List.of("active", "pending"))

// IS NULL / IS TRUE / IS FALSE
.is("deleted_at", null)
.isNull("deleted_at")       // shorthand for .is("deleted_at", null)
.is("verified", true)

// Negate any operator
.not("status", "eq", "deleted")
.not("role", "in", "(admin,mod)")

// OR — combine conditions
.or("age.gt.18,status.eq.active")
```

**Supported value types in filters:** `String`, `Number` (Integer, Long, Double, etc.), `Boolean`, `Instant`, `LocalDate`, `Enum`.

---

### Ordering and Pagination

```java
// Order by a column
.order("created_at", "desc")
.order("name", "asc")

// Limit and offset
.limit(20)
.offset(40)

// Range — returns rows 0 through 19 inclusive (20 rows)
.range(0, 19)
```

---

### INSERT

```java
User newUser = new User();
newUser.name  = "Alice";
newUser.email = "alice@example.com";

List<User> created = client.postgrest()
    .from("users")
    .insert(newUser, User.class);

User alice = created.get(0); // contains server-generated id, created_at, etc.
```

**Bulk insert** — pass a `List`:

```java
List<User> created = client.postgrest()
    .from("users")
    .insert(List.of(user1, user2, user3), User.class);
```

---

### UPSERT

On primary key conflict, the existing row is updated rather than throwing a unique constraint error.

```java
List<User> upserted = client.postgrest()
    .from("users")
    .upsert(user, User.class);
```

---

### UPDATE

At least one filter **must** be set before calling `update()`. This is a safety guard against accidental full-table updates — an `IllegalStateException` is thrown otherwise.

```java
Map<String, Object> patch = Map.of("name", "Alice Smith");

List<User> updated = client.postgrest()
    .from("users")
    .eq("id", "abc-123")
    .update(patch, User.class);
```

---

### DELETE

At least one filter **must** be set before calling `delete()` or `deleteReturning()`.

```java
// Delete without returning data
client.postgrest()
    .from("users")
    .eq("id", "abc-123")
    .delete();

// Delete and return the deleted rows
List<User> deleted = client.postgrest()
    .from("users")
    .eq("id", "abc-123")
    .deleteReturning(User.class);
```

---

### RPC — Postgres Functions

Call a Postgres function via the PostgREST RPC endpoint.

```sql
-- Example Postgres function
CREATE FUNCTION get_user_by_email(user_email text)
RETURNS users LANGUAGE sql AS $$
    SELECT * FROM users WHERE email = user_email LIMIT 1;
$$;
```

```java
// Single result
User user = client.postgrest()
    .rpc("get_user_by_email", Map.of("user_email", "alice@example.com"), User.class);

// Multiple results (SETOF / RETURNS TABLE)
List<User> users = client.postgrest()
    .rpcList("search_users", Map.of("query", "alice"), User.class);

// No arguments
User user = client.postgrest()
    .rpc("get_current_user", null, User.class);
```

---

### Immutable Builder — Branching Queries

Because every builder method returns a new instance, you can safely branch from a base query:

```java
PostgrestQueryBuilder activeUsers = client.postgrest()
    .from("users")
    .eq("active", true);

// Both queries are independent — neither affects the other
List<User> admins = activeUsers.eq("role", "admin").selectList(User.class);
List<User> mods   = activeUsers.eq("role", "mod").selectList(User.class);
```

---

## Error Handling

All errors surface as `SupabaseException`, a `RuntimeException` subclass. You can catch it selectively or let it propagate.

```java
try {
    User user = client.postgrest()
        .from("users")
        .eq("id", "does-not-exist")
        .single()
        .selectSingle(User.class);

} catch (SupabaseException e) {
    System.out.println(e.getStatusCode());     // HTTP status code (406, 409, 500, etc.)
    System.out.println(e.getMessage());        // Human-readable message
    System.out.println(e.getResponseBody());   // Raw response body from the server
    System.out.println(e.getPostgrestCode());  // PostgREST error code e.g. "PGRST116"
    System.out.println(e.getDetails());        // PostgREST details field
    System.out.println(e.getHint());           // PostgREST hint field
}
```

| Status Code | Meaning |
|---|---|
| HTTP 4xx / 5xx | PostgREST or Supabase error — check `getPostgrestCode()` and `getDetails()` |
| `-1` | Network failure (no connection, timeout) |
| `0` | JSON deserialization failure (POJO field mismatch) |

### Common PostgREST error codes

| Code | Meaning |
|---|---|
| `PGRST116` | 406 — zero rows matched on a `.single()` query |
| `23505` | 409 — unique constraint violation on insert |
| `42501` | 403 — Row Level Security policy violation |

---

## Dependencies

| Dependency | Purpose |
|---|---|
| [OkHttp](https://square.github.io/okhttp/) | HTTP client — connection pooling, timeouts |
| [Jackson Databind](https://github.com/FasterXML/jackson-databind) | JSON serialization and deserialization |

---

## Contributing

Contributions are welcome. Please open an issue before submitting a pull request for large changes.

### Setup

```bash
git clone https://github.com/jayesh1126/supabase-java.git
cd supabase-java
mvn install
```

### Running tests

```bash
mvn test
```

### Code style

- Java 17+
- All public methods must have Javadoc
- No `RuntimeException` — use `SupabaseException` for network/HTTP errors, `IllegalArgumentException` for invalid input, `IllegalStateException` for invalid builder state
- Builder methods must be immutable — always return a new instance

---

## Roadmap

- [x] **Auth (GoTrue)** — signup, login, logout, token refresh, session management
- [ ] **Storage** — bucket management, file upload and download
- [ ] **Edge Functions** — invoke Supabase Edge Functions
- [ ] **Realtime** — WebSocket subscriptions to database changes
- [ ] **Count / HEAD support** — `Prefer: count=exact`, `Content-Range` header parsing, `PagedResult<T>` wrapper
- [ ] **`or()` typed DSL** — `or(Consumer<OrBuilder>)` for type-safe OR expressions
- [ ] **Full-text search operators** — `fts`, `plfts`, `phfts`, `wfts`
- [ ] **Array operators** — `cs` (contains), `cd` (contained by), and range operators
- [ ] **`order()` null handling** — `nullsfirst` / `nullslast` support
- [ ] **Column validation** — regex guard against invalid identifiers in filter methods
- [ ] **Retry / resilience** — configurable retry with exponential backoff, `429` handling
- [ ] **Async API** — `CompletableFuture`-based terminal methods
- [ ] **Published to Maven Central**
- [ ] **More tests files, mocks**
- [ ] **CI/CD via GitHub Actions**
- [ ] **Easy quickstart via docker**

---

## License

MIT — see [LICENSE](LICENSE) for details.

---

## Acknowledgements

- [Supabase](https://supabase.com) — the platform this client targets
- [supabase-js](https://github.com/supabase/supabase-js) — the official JavaScript SDK whose API design this library mirrors
- [PostgREST](https://postgrest.org) — the REST API layer Supabase exposes for database access