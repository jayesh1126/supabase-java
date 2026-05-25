package io.github.jayesh1126.supabase;

import io.github.jayesh1126.supabase.auth.AuthClient;
import io.github.jayesh1126.supabase.postgrest.PostgrestClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SupabaseClientTest {

    @Test
    void shouldCreateClientAndExposeSubClients() {
        SupabaseClient client =
                new SupabaseClient(
                        "https://example.supabase.co",
                        "anon-key"
                );

        assertNotNull(client, "client must be created");

        assertNotNull(
                client.auth(),
                "auth() must return non-null AuthClient"
        );

        assertNotNull(
                client.postgrest(),
                "postgrest() must return non-null PostgrestClient"
        );

        assertNotNull(
                client.postgrest().from("cities"),
                "from(table) must return non-null query builder"
        );
    }

    @Test
    void shouldCreateClientWithAccessToken() {
        SupabaseClient client =
                new SupabaseClient(
                        "https://example.supabase.co",
                        "anon-key",
                        "user-access-token"
                );

        assertNotNull(client);

        assertNotNull(
                client.auth(),
                "auth() must be available for authenticated clients"
        );

        assertNotNull(
                client.postgrest(),
                "postgrest() must be available for authenticated clients"
        );
    }

    @Test
    void shouldThrowWhenSupabaseUrlIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new SupabaseClient(null, "anon-key")
        );

        assertEquals(
                "supabaseUrl must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenApiKeyIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new SupabaseClient(
                        "https://example.supabase.co",
                        null
                )
        );

        assertEquals(
                "supabaseApiKey must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenSupabaseUrlIsInvalid() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SupabaseClient(
                        "not-a-valid-url",
                        "anon-key"
                )
        );

        assertTrue(
                exception.getMessage().contains("Invalid Supabase URL"),
                "exception message must indicate invalid URL"
        );
    }

    @Test
    void shouldCreateNewClientWithAccessToken() {
        SupabaseClient baseClient =
                new SupabaseClient(
                        "https://example.supabase.co",
                        "anon-key"
                );

        SupabaseClient authenticatedClient =
                baseClient.withAccessToken("jwt-token");

        assertNotNull(authenticatedClient);

        assertNotSame(
                baseClient,
                authenticatedClient,
                "withAccessToken() must create a new client instance"
        );
    }

    @Test
    void shouldReuseSubClientTypesAcrossAuthenticatedClients() {
        SupabaseClient baseClient =
                new SupabaseClient(
                        "https://example.supabase.co",
                        "anon-key"
                );

        SupabaseClient authenticatedClient =
                baseClient.withAccessToken("jwt-token");

        assertInstanceOf(
                AuthClient.class,
                authenticatedClient.auth()
        );

        assertInstanceOf(
                PostgrestClient.class,
                authenticatedClient.postgrest()
        );
    }

    @Test
    void shouldCreateIndependentAuthenticatedClients() {
        SupabaseClient baseClient =
                new SupabaseClient(
                        "https://example.supabase.co",
                        "anon-key"
                );

        SupabaseClient clientA =
                baseClient.withAccessToken("token-a");

        SupabaseClient clientB =
                baseClient.withAccessToken("token-b");

        assertNotSame(
                clientA,
                clientB,
                "each authenticated client must be a distinct instance"
        );
    }

    @Test
    void shouldAllowMultipleCallsToSubClients() {
        SupabaseClient client =
                new SupabaseClient(
                        "https://example.supabase.co",
                        "anon-key"
                );

        assertDoesNotThrow(client::auth);

        assertDoesNotThrow(client::postgrest);

        assertDoesNotThrow(
                () -> client.postgrest().from("users")
        );
    }
}