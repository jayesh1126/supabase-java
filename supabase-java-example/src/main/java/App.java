import io.github.cdimascio.dotenv.Dotenv;
import io.github.jayesh1126.supabase.SupabaseClient;
import io.github.jayesh1126.supabase.auth.model.AuthResponse;
import io.github.jayesh1126.supabase.auth.model.User;
import io.github.jayesh1126.supabase.exception.SupabaseException;

import java.util.List;
import java.util.Map;

public class App {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();

        String url = dotenv.get("SUPABASE_URL");
        String key = dotenv.get("SUPABASE_API_KEY");

        SupabaseClient client = new SupabaseClient(url, key);

        AuthResponse auth = testAuthentication(client);

        SupabaseClient authenticatedClient =
                client.withAccessToken(auth.accessToken());

        testAuthenticatedUser(authenticatedClient);

        // Run each section independently.
        // Comment out sections you haven't set up yet.
        testSelectAll(client);
        testSelectColumns(client);
        testSelectSingle(client);
        testFilters(client);
        testOrderAndPagination(client);
        testInsertSingle(client);
        testInsertBulk(client);
        testUpsert(client);
        testUpdate(client);
        testDelete(client);
        testRpc(client);
    }

    // -------------------------------------------------------
    // 1. Fetch all rows — no filters, no projection
    // -------------------------------------------------------
    static void testSelectAll(SupabaseClient client) {
        section("SELECT ALL ROWS");
        try {
            List<Test> results = client.postgrest()
                    .from("test_table")
                    .selectList(Test.class);

            log("Row count: " + results.size());
            results.forEach(System.out::println);

        } catch (SupabaseException e) {
            error(e);
        }
    }

    // -------------------------------------------------------
    // 2. Select only specific columns
    //    Only "id" and "name" are returned — "count" and
    //    "created_at" will be null/default in the record.
    // -------------------------------------------------------
    static void testSelectColumns(SupabaseClient client) {
        section("SELECT SPECIFIC COLUMNS");
        try {
            List<Test> results = client.postgrest()
                    .from("test_table")
                    .select("id,name")
                    .selectList(Test.class);

            log("Fetched " + results.size() + " rows with id and name only");
            results.forEach(r -> log("  id=" + r.id() + " name=" + r.name()));

        } catch (SupabaseException e) {
            error(e);
        }
    }

    // -------------------------------------------------------
    // 3. Fetch a single row by name
    //    .single() requires exactly one row to match.
    //    PostgREST returns 406 for zero rows, 409 for multiple.
    //    Change the name to one that exists and is unique in your table.
    // -------------------------------------------------------
    static void testSelectSingle(SupabaseClient client) {
        section("SELECT SINGLE ROW");
        try {
            Test result = client.postgrest()
                    .from("test_table")
                    .eq("name", "Charlie")
                    .single()
                    .selectSingle(Test.class);

            log("Found: " + result);

        } catch (SupabaseException e) {
            // 406 = no row matched, 409 = more than one row matched
            error(e);
        }
    }

    // -------------------------------------------------------
    // 4. Filters — combined, chained
    //    All filters are AND-ed together.
    // -------------------------------------------------------
    static void testFilters(SupabaseClient client) {
        section("FILTERS");

        // 4a. gt + lt on count (range on a single column)
        try {
            log("--- count between 5 and 20 (gt + lt) ---");
            List<Test> results = client.postgrest()
                    .from("test_table")
                    .gt("count", 5)
                    .lt("count", 20)
                    .selectList(Test.class);
            results.forEach(r -> log("  " + r));
        } catch (SupabaseException e) {
            error(e);
        }

        // 4b. ilike — case-insensitive name search
        try {
            log("--- name ilike 'a%' ---");
            List<Test> results = client.postgrest()
                    .from("test_table")
                    .ilike("name", "a%")
                    .selectList(Test.class);
            results.forEach(r -> log("  " + r));
        } catch (SupabaseException e) {
            error(e);
        }

        // 4c. in — match against a list of names
        try {
            log("--- name in (Alice, Bob) ---");
            List<Test> results = client.postgrest()
                    .from("test_table")
                    .in("name", List.of("Alice", "Bob"))
                    .selectList(Test.class);
            results.forEach(r -> log("  " + r));
        } catch (SupabaseException e) {
            error(e);
        }

        // 4d. isNull — rows where count IS NULL
        try {
            log("--- count is null ---");
            List<Test> results = client.postgrest()
                    .from("test_table")
                    .isNull("count")
                    .selectList(Test.class);
            log("Rows with null count: " + results.size());
        } catch (SupabaseException e) {
            error(e);
        }

        // 4e. not — rows where name is NOT 'Alice'
        try {
            log("--- name not eq Alice ---");
            List<Test> results = client.postgrest()
                    .from("test_table")
                    .not("name", "eq", "Alice")
                    .selectList(Test.class);
            results.forEach(r -> log("  " + r));
        } catch (SupabaseException e) {
            error(e);
        }

        // 4f. or — name is Alice OR name is Bob
        try {
            log("--- or: name eq Alice OR name eq Bob ---");
            List<Test> results = client.postgrest()
                    .from("test_table")
                    .or("name.eq.Alice,name.eq.Bob")
                    .selectList(Test.class);
            results.forEach(r -> log("  " + r));
        } catch (SupabaseException e) {
            error(e);
        }

        // 4g. Combined filter — active rows named Alice with count >= 10
        try {
            log("--- combined: name=Alice AND count >= 10 ---");
            List<Test> results = client.postgrest()
                    .from("test_table")
                    .eq("name", "Alice")
                    .gte("count", 10)
                    .selectList(Test.class);
            results.forEach(r -> log("  " + r));
        } catch (SupabaseException e) {
            error(e);
        }
    }

    // -------------------------------------------------------
    // 5. Order, limit, offset, range
    // -------------------------------------------------------
    static void testOrderAndPagination(SupabaseClient client) {
        section("ORDER AND PAGINATION");

        // Order by count descending, take top 3
        try {
            log("--- top 3 by count desc ---");
            List<Test> results = client.postgrest()
                    .from("test_table")
                    .order("count", "desc")
                    .limit(3)
                    .selectList(Test.class);
            results.forEach(r -> log("  " + r));
        } catch (SupabaseException e) {
            error(e);
        }

        // Order by name asc with offset — second page of 2
        try {
            log("--- name asc, offset 2, limit 2 ---");
            List<Test> results = client.postgrest()
                    .from("test_table")
                    .order("name", "asc")
                    .offset(2)
                    .limit(2)
                    .selectList(Test.class);
            results.forEach(r -> log("  " + r));
        } catch (SupabaseException e) {
            error(e);
        }

        // range — rows 0 through 4 inclusive (5 rows)
        try {
            log("--- range(0, 4) — first 5 rows ---");
            List<Test> results = client.postgrest()
                    .from("test_table")
                    .range(0, 4)
                    .selectList(Test.class);
            results.forEach(r -> log("  " + r));
        } catch (SupabaseException e) {
            error(e);
        }
    }

    // -------------------------------------------------------
    // 6. Insert a single row
    //    The returned list contains the inserted row with
    //    server-generated fields (id, created_at) populated.
    // -------------------------------------------------------
    static void testInsertSingle(SupabaseClient client) {
        section("INSERT SINGLE");
        try {
            // Use a Map so you only send the fields you want —
            // id and created_at are generated by Supabase.
            Map<String, Object> newRow = Map.of(
                    "name",  "Charlie",
                    "count", 7
            );

            List<Test> inserted = client.postgrest()
                    .from("test_table")
                    .insert(newRow, Test.class);

            log("Inserted " + inserted.size() + " row(s):");
            inserted.forEach(r -> log("  " + r));

        } catch (SupabaseException e) {
            error(e);
        }
    }

    // -------------------------------------------------------
    // 7. Bulk insert — multiple rows in one request
    // -------------------------------------------------------
    static void testInsertBulk(SupabaseClient client) {
        section("INSERT BULK");
        try {
            List<Map<String, Object>> rows = List.of(
                    Map.of("name", "Diana",  "count", 3),
                    Map.of("name", "Edward", "count", 15),
                    Map.of("name", "Fiona",  "count", 22)
            );

            List<Test> inserted = client.postgrest()
                    .from("test_table")
                    .insert(rows, Test.class);

            log("Bulk inserted " + inserted.size() + " row(s):");
            inserted.forEach(r -> log("  " + r));

        } catch (SupabaseException e) {
            error(e);
        }
    }

    // -------------------------------------------------------
    // 8. Upsert — inserts if no conflict, updates on PK match
    //    Change the id to one that already exists to test the
    //    update path. Use a new id to test the insert path.
    // -------------------------------------------------------
    static void testUpsert(SupabaseClient client) {
        section("UPSERT");
        try {
            // If a row with this name already exists and name is a unique key,
            // it will be updated. Otherwise it will be inserted.
            // Adjust fields to match your table's unique constraints.
            Map<String, Object> row = Map.of(
                    "name",  "Charlie",
                    "count", 99
            );

            List<Test> upserted = client.postgrest()
                    .from("test_table")
                    .upsert(row, Test.class);

            log("Upserted " + upserted.size() + " row(s):");
            upserted.forEach(r -> log("  " + r));

        } catch (SupabaseException e) {
            error(e);
        }
    }

    // -------------------------------------------------------
    // 9. Update — patch matching rows, return updated rows
    //    At least one filter is required.
    // -------------------------------------------------------
    static void testUpdate(SupabaseClient client) {
        section("UPDATE");
        try {
            Map<String, Object> patch = Map.of("count", 100);

            List<Test> updated = client.postgrest()
                    .from("test_table")
                    .eq("name", "Charlie")
                    .update(patch, Test.class);

            log("Updated " + updated.size() + " row(s):");
            updated.forEach(r -> log("  " + r));

        } catch (SupabaseException e) {
            error(e);
        }
    }

    // -------------------------------------------------------
    // 10. Delete
    //     At least one filter is required.
    //     deleteReturning echoes back what was deleted.
    // -------------------------------------------------------
    static void testDelete(SupabaseClient client) {
        section("DELETE");

        // Delete without returning data
        try {
            log("--- delete Diana (no return) ---");
            client.postgrest()
                    .from("test_table")
                    .eq("name", "Diana")
                    .delete();
            log("Deleted successfully");
        } catch (SupabaseException e) {
            error(e);
        }

        // Delete and return the deleted rows
        try {
            log("--- delete Edward (with return) ---");
            List<Test> deleted = client.postgrest()
                    .from("test_table")
                    .eq("name", "Edward")
                    .deleteReturning(Test.class);

            log("Deleted " + deleted.size() + " row(s):");
            deleted.forEach(r -> log("  " + r));
        } catch (SupabaseException e) {
            error(e);
        }
    }

    // -------------------------------------------------------
    // 11. RPC — call a Postgres function
    //
    //     Create this function in your Supabase SQL editor first:
    //
    //     CREATE OR REPLACE FUNCTION get_rows_by_min_count(min_count int)
    //     RETURNS SETOF test_table LANGUAGE sql AS $$
    //         SELECT * FROM test_table WHERE count >= min_count ORDER BY count ASC;
    //     $$;
    //
    //     And a scalar function for the single-result test:
    //
    //     CREATE OR REPLACE FUNCTION get_total_count()
    //     RETURNS int LANGUAGE sql AS $$
    //         SELECT COALESCE(SUM(count), 0) FROM test_table;
    //     $$;
    // -------------------------------------------------------
    static void testRpc(SupabaseClient client) {
        section("RPC");

        // rpcList — function returns SETOF (multiple rows)
        try {
            log("--- rpcList: get_rows_by_min_count(min_count=10) ---");
            List<Test> results = client.postgrest()
                    .rpcList("get_rows_by_min_count", Map.of("min_count", 10), Test.class);

            log("Returned " + results.size() + " row(s):");
            results.forEach(r -> log("  " + r));

        } catch (SupabaseException e) {
            error(e);
        }

        // rpc — function returns a scalar, wrap it in a simple record
        try {
            log("--- rpc: get_total_count() ---");
            TotalCount total = client.postgrest()
                    .rpc("get_total_count", null, TotalCount.class);

            log("Total count across all rows: " + total);

        } catch (SupabaseException e) {
            // Note: scalar functions return a plain value, not a JSON object.
            // If deserialization fails, PostgREST may need a wrapper or the
            // function should return a JSON object instead.
            error(e);
        }
    }

    // -------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------

    private static void section(String name) {
        System.out.println("\n" + "=".repeat(55));
        System.out.println("  " + name);
        System.out.println("=".repeat(55));
    }

    private static void log(String msg) {
        System.out.println("[INFO]  " + msg);
    }

    private static void error(SupabaseException e) {
        System.out.println("[ERROR] status=" + e.getStatusCode()
                + " message=" + e.getMessage());
        if (e.getPostgrestCode() != null)
            System.out.println("        postgrestCode=" + e.getPostgrestCode());
        if (e.getDetails() != null)
            System.out.println("        details="       + e.getDetails());
        if (e.getHint() != null)
            System.out.println("        hint="          + e.getHint());
    }

    static AuthResponse testAuthentication(SupabaseClient client) {
        section("AUTHENTICATION");

        try {
            String email = "test@example.com";
            String password = "password123";

            // ----------------------------
            // SIGN UP
            // ----------------------------
            log("--- sign up ---");

            AuthResponse signup = client.auth()
                    .signUpWithEmail(email, password);

            log("Signup successful");
            log("User ID: " + signup.user().id());

            // ----------------------------
            // SIGN IN
            // ----------------------------
            log("--- sign in ---");

            AuthResponse login = client.auth()
                    .signInWithEmail(email, password);

            log("Login successful");
            log("Access token: " + login.accessToken());
            log("Refresh token: " + login.refreshToken());

            return login;

        } catch (SupabaseException e) {
            error(e);
            throw e;
        }
    }

    static void testAuthenticatedUser(SupabaseClient client) {
        section("AUTHENTICATED USER");

        try {
            User user = client.auth().getUser();

            log("Authenticated user:");
            log("ID: " + user.id());
            log("Email: " + user.email());

        } catch (SupabaseException e) {
            error(e);
        }
    }

    static void testRefreshToken(SupabaseClient client, String refreshToken) {
        section("REFRESH TOKEN");

        try {
            AuthResponse refreshed = client.auth()
                    .refreshAccessToken(refreshToken);

            log("New access token:");
            log(refreshed.accessToken());

        } catch (SupabaseException e) {
            error(e);
        }
    }

    static void testLogout(SupabaseClient client) {
        section("LOGOUT");

        try {
            client.auth().signOut();
            log("Signed out successfully");

        } catch (SupabaseException e) {
            error(e);
        }
    }
}
