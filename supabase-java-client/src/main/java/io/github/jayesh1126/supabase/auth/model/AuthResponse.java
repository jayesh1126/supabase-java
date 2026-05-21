package io.github.jayesh1126.supabase.auth.model;

public record AuthResponse(
        User user,
        Session session
) {}