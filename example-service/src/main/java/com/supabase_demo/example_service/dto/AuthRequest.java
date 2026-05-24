package com.supabase_demo.example_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
    private String email;

    @ToString.Exclude
    private String password;
}

