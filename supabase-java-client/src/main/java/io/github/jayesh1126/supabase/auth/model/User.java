package io.github.jayesh1126.supabase.auth.model;

public record User(
        String id,
        String email,
        String role
) {}
