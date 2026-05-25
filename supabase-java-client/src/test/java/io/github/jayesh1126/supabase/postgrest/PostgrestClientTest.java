package io.github.jayesh1126.supabase.postgrest;

import io.github.jayesh1126.supabase.core.SupabaseHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class PostgrestClientTest {

    private SupabaseHttpClient http;
    private ObjectMapper objectMapper;

    private PostgrestClient postgrestClient;

    @BeforeEach
    void setUp() {
        http = mock(SupabaseHttpClient.class);
        objectMapper = new ObjectMapper();

        postgrestClient = new PostgrestClient(
                http,
                objectMapper
        );
    }

    @Test
    void shouldCreatePostgrestClient() {
        assertNotNull(postgrestClient);
    }

    @Test
    void shouldThrowWhenHttpClientIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new PostgrestClient(
                        null,
                        objectMapper
                )
        );

        assertEquals(
                "http must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenObjectMapperIsNull() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new PostgrestClient(
                        http,
                        null
                )
        );

        assertEquals(
                "objectMapper must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldCreateQueryBuilderForTable() {

        PostgrestQueryBuilder builder =
                postgrestClient.from("users");

        assertNotNull(builder);
    }

    @Test
    void shouldCreateQueryBuilderForSchemaQualifiedTable() {

        PostgrestQueryBuilder builder =
                postgrestClient.from(
                        "private",
                        "users"
                );

        assertNotNull(builder);
    }

    @Test
    void shouldThrowWhenTableIsNull() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> postgrestClient.from(null)
        );

        assertEquals(
                "table must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenTableIsBlank() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postgrestClient.from(" ")
        );

        assertEquals(
                "table must not be blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenSchemaIsNull() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> postgrestClient.from(
                        null,
                        "users"
                )
        );

        assertEquals(
                "schema must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenSchemaIsBlank() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postgrestClient.from(
                        " ",
                        "users"
                )
        );

        assertEquals(
                "schema must not be blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenSchemaContainsInvalidCharacters() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postgrestClient.from(
                        "private;",
                        "users"
                )
        );

        assertEquals(
                "schema contains invalid characters",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenTableContainsInvalidCharacters() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postgrestClient.from(
                        "users;DROP TABLE users"
                )
        );

        assertEquals(
                "table contains invalid characters",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenRpcFunctionNameIsNull() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> postgrestClient.rpc(
                        null,
                        null,
                        String.class
                )
        );

        assertEquals(
                "functionName must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenRpcReturnTypeIsNull() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> postgrestClient.rpc(
                        "get_user",
                        null,
                        null
                )
        );

        assertEquals(
                "returnType must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenRpcListFunctionNameIsNull() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> postgrestClient.rpcList(
                        null,
                        null,
                        String.class
                )
        );

        assertEquals(
                "functionName must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldThrowWhenRpcListReturnTypeIsNull() {

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> postgrestClient.rpcList(
                        "search_users",
                        null,
                        null
                )
        );

        assertEquals(
                "returnType must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldAllowValidIdentifiers() {

        assertDoesNotThrow(
                () -> postgrestClient.from("users")
        );

        assertDoesNotThrow(
                () -> postgrestClient.from(
                        "private",
                        "audit_logs"
                )
        );

        assertDoesNotThrow(
                () -> postgrestClient.from("user_profiles")
        );
    }

    @Test
    void shouldRejectIdentifiersContainingSpaces() {

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postgrestClient.from("user profiles")
        );

        assertEquals(
                "table contains invalid characters",
                exception.getMessage()
        );
    }
}