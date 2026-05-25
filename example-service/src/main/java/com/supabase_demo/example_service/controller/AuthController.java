package com.supabase_demo.example_service.controller;

import com.supabase_demo.example_service.dto.AuthRequest;
import com.supabase_demo.example_service.dto.AuthResponse;
import com.supabase_demo.example_service.dto.LogoutRequest;
import com.supabase_demo.example_service.dto.RefreshTokenRequest;
import io.github.jayesh1126.supabase.SupabaseClient;
import io.github.jayesh1126.supabase.exception.SupabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private SupabaseClient supabaseClient;

    /**
     * Sign up a new user with email and password
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody AuthRequest request) {
        try {
            io.github.jayesh1126.supabase.auth.model.AuthResponse supabaseResponse = supabaseClient.auth()
                    .signUpWithEmail(request.getEmail(), request.getPassword());

            AuthResponse response = mapToAuthResponse(supabaseResponse);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SupabaseException e) {
            logger.error("signUp email={} - SupabaseException status={} message={}", request.getEmail(), e.getStatusCode(), e.getMessage(), e);
            int status = resolveHttpStatus(e.getStatusCode());
            String msg = e.getMessage() != null ? e.getMessage() : "Supabase error";
            return ResponseEntity.status(status).body(Map.of("message", msg));
        } catch (IllegalArgumentException e) {
            logger.error("signUp email={} - IllegalArgumentException: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("signUp email={} - unexpected exception", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Sign in an existing user with email and password
     */
    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@RequestBody AuthRequest request) {
        try {
            io.github.jayesh1126.supabase.auth.model.AuthResponse supabaseResponse = supabaseClient.auth()
                    .signInWithEmail(request.getEmail(), request.getPassword());

            AuthResponse response = mapToAuthResponse(supabaseResponse);
            return ResponseEntity.ok(response);
        } catch (SupabaseException e) {
            logger.error("signIn email={} - SupabaseException status={} message={}", request.getEmail(), e.getStatusCode(), e.getMessage(), e);
            int status = resolveHttpStatus(e.getStatusCode());
            String msg = e.getMessage() != null ? e.getMessage() : "Supabase error";
            return ResponseEntity.status(status).body(Map.of("message", msg));
        } catch (IllegalArgumentException e) {
            logger.error("signIn email={} - IllegalArgumentException: {}", request.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("signIn email={} - unexpected exception", request.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Refresh an access token using a refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            io.github.jayesh1126.supabase.auth.model.AuthResponse supabaseResponse = supabaseClient.auth()
                    .refreshAccessToken(request.getRefreshToken());

            AuthResponse response = mapToAuthResponse(supabaseResponse);
            return ResponseEntity.ok(response);
        } catch (SupabaseException e) {
            logger.error("refreshToken - SupabaseException status={} message={}", e.getStatusCode(), e.getMessage(), e);
            int status = resolveHttpStatus(e.getStatusCode());
            String msg = e.getMessage() != null ? e.getMessage() : "Supabase error";
            return ResponseEntity.status(status).body(Map.of("message", msg));
        } catch (IllegalArgumentException e) {
            logger.error("refreshToken - IllegalArgumentException: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("refreshToken - unexpected exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Sign out a user using their access token
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        try {
            SupabaseClient authenticatedClient = supabaseClient.withAccessToken(request.getAccessToken());
            authenticatedClient.auth().signOut();
            return ResponseEntity.noContent().build();
        } catch (SupabaseException e) {
            logger.error("logout - SupabaseException status={} message={}", e.getStatusCode(), e.getMessage(), e);
            int status = resolveHttpStatus(e.getStatusCode());
            String msg = e.getMessage() != null ? e.getMessage() : "Supabase error";
            return ResponseEntity.status(status).body(Map.of("message", msg));
        } catch (IllegalArgumentException e) {
            logger.error("logout - IllegalArgumentException: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("logout - unexpected exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Helper method to map Supabase AuthResponse to our DTO
     */
    private AuthResponse mapToAuthResponse(io.github.jayesh1126.supabase.auth.model.AuthResponse supabaseResponse) {
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                supabaseResponse.user().id(),
                supabaseResponse.user().email(),
                supabaseResponse.user().role()
        );

        return new AuthResponse(
                supabaseResponse.accessToken(),
                supabaseResponse.refreshToken(),
                supabaseResponse.expiresIn(),
                supabaseResponse.tokenType(),
                userInfo
        );
    }

    /**
     * Map SupabaseException status codes to sensible HTTP status codes for responses.
     * - HTTP 100-599: returned as-is
     * - -1 (network failure): 503 Service Unavailable
     * - 0 (client-side error): 500 Internal Server Error
     * - otherwise: 500
     */
    private int resolveHttpStatus(int supabaseStatus) {
        if (supabaseStatus >= 100 && supabaseStatus <= 599) {
            return supabaseStatus;
        }
        if (supabaseStatus == -1) {
            return HttpStatus.SERVICE_UNAVAILABLE.value();
        }
        if (supabaseStatus == 0) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }
}
