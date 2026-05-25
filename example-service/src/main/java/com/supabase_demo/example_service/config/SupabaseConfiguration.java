package com.supabase_demo.example_service.config;

import io.github.jayesh1126.supabase.SupabaseClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SupabaseConfiguration {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anon-key}")
    private String supabaseAnonKey;

    @Bean
    public SupabaseClient supabaseClient() {
        return new SupabaseClient(supabaseUrl, supabaseAnonKey);
    }
}

