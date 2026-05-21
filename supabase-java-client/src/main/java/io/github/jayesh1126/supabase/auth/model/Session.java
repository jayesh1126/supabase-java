package io.github.jayesh1126.supabase.auth.model;

public record Session(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        User user
) {}