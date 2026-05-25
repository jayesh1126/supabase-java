package io.github.jayesh1126.supabase.auth.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Authentication response returned by Supabase GoTrue.
 *
 * <p>Returned from:
 * <ul>
 *     <li>Sign in</li>
 *     <li>Token refresh</li>
 *     <li>Some signup flows</li>
 * </ul>
 */
public record AuthResponse(

        /**
         * JWT access token used for authenticated requests.
         */
        @JsonProperty("access_token")
        String accessToken,

        /**
         * Refresh token used to obtain a new access token.
         */
        @JsonProperty("refresh_token")
        String refreshToken,

        /**
         * Token expiration time in seconds.
         */
        @JsonProperty("expires_in")
        long expiresIn,

        /**
         * Token type returned by GoTrue.
         * Usually "bearer".
         */
        @JsonProperty("token_type")
        String tokenType,

        /**
         * Authenticated Supabase user.
         */
        User user

) {}