package com.supabase_demo.example_service.controller;

import com.supabase_demo.example_service.model.City;
import io.github.jayesh1126.supabase.SupabaseClient;
import io.github.jayesh1126.supabase.exception.SupabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cities")
public class CitiesController {

    private static final Logger logger = LoggerFactory.getLogger(CitiesController.class);

    @Autowired
    private SupabaseClient supabaseClient;

    /**
     * Get all cities - requires access token in Authorization header
     */
    @GetMapping
    public ResponseEntity<?> getAllCities(@RequestHeader("Authorization") String authHeader) {
        try {
            String accessToken = extractAccessToken(authHeader);
            SupabaseClient authenticatedClient = supabaseClient.withAccessToken(accessToken);

            List<City> cities = authenticatedClient.postgrest()
                    .from("cities")
                    .selectList(City.class);

            return ResponseEntity.ok(cities);
        } catch (SupabaseException e) {
            logger.error("getAllCities - SupabaseException status={} message={}", e.getStatusCode(), e.getMessage(), e);
            int status = resolveHttpStatus(e.getStatusCode());
            String msg = e.getMessage() != null ? e.getMessage() : "Supabase error";
            return ResponseEntity.status(status).body(Map.of("message", msg));
        } catch (IllegalArgumentException e) {
            logger.error("getAllCities - IllegalArgumentException: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("getAllCities - unexpected exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Get a city by ID - requires access token in Authorization header
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCityById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String accessToken = extractAccessToken(authHeader);
            SupabaseClient authenticatedClient = supabaseClient.withAccessToken(accessToken);

            City city = authenticatedClient.postgrest()
                    .from("cities")
                    .eq("id", id)
                    .single()
                    .selectSingle(City.class);

            return ResponseEntity.ok(city);
        } catch (SupabaseException e) {
            logger.error("getCityById id={} - SupabaseException status={} message={}", id, e.getStatusCode(), e.getMessage(), e);
            int status = resolveHttpStatus(e.getStatusCode());
            String msg = e.getMessage() != null ? e.getMessage() : "Supabase error";
            return ResponseEntity.status(status).body(Map.of("message", msg));
        } catch (IllegalArgumentException e) {
            logger.error("getCityById id={} - IllegalArgumentException: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("getCityById id={} - unexpected exception", id, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Search cities by country - requires access token in Authorization header
     */
    @GetMapping("/search/country/{country}")
    public ResponseEntity<?> getCitiesByCountry(
            @PathVariable String country,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String accessToken = extractAccessToken(authHeader);
            SupabaseClient authenticatedClient = supabaseClient.withAccessToken(accessToken);

            List<City> cities = authenticatedClient.postgrest()
                    .from("cities")
                    .eq("country", country)
                    .selectList(City.class);

            return ResponseEntity.ok(cities);
        } catch (SupabaseException e) {
            logger.error("getCitiesByCountry country={} - SupabaseException status={} message={}", country, e.getStatusCode(), e.getMessage(), e);
            int status = resolveHttpStatus(e.getStatusCode());
            String msg = e.getMessage() != null ? e.getMessage() : "Supabase error";
            return ResponseEntity.status(status).body(Map.of("message", msg));
        } catch (IllegalArgumentException e) {
            logger.error("getCitiesByCountry country={} - IllegalArgumentException: {}", country, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("getCitiesByCountry country={} - unexpected exception", country, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Create a new city - requires access token in Authorization header
     */
    @PostMapping
    public ResponseEntity<?> createCity(
            @RequestBody City city,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String accessToken = extractAccessToken(authHeader);
            SupabaseClient authenticatedClient = supabaseClient.withAccessToken(accessToken);

            List<City> createdCities = authenticatedClient.postgrest()
                    .from("cities")
                    .insert(city, City.class);

            if (!createdCities.isEmpty()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(createdCities.get(0));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Creation failed"));
        } catch (SupabaseException e) {
            logger.error("createCity city={} - SupabaseException status={} message={}", city, e.getStatusCode(), e.getMessage(), e);
            int status = resolveHttpStatus(e.getStatusCode());
            String msg = e.getMessage() != null ? e.getMessage() : "Supabase error";
            return ResponseEntity.status(status).body(Map.of("message", msg));
        } catch (IllegalArgumentException e) {
            logger.error("createCity - IllegalArgumentException: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("createCity - unexpected exception", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Update a city - requires access token in Authorization header
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCity(
            @PathVariable Long id,
            @RequestBody City city,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String accessToken = extractAccessToken(authHeader);
            SupabaseClient authenticatedClient = supabaseClient.withAccessToken(accessToken);

            List<City> updatedCities = authenticatedClient.postgrest()
                    .from("cities")
                    .eq("id", id)
                    .update(city, City.class);

            if (!updatedCities.isEmpty()) {
                return ResponseEntity.ok(updatedCities.get(0));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Not found"));
        } catch (SupabaseException e) {
            logger.error("updateCity id={} city={} - SupabaseException status={} message={}", id, city, e.getStatusCode(), e.getMessage(), e);
            int status = resolveHttpStatus(e.getStatusCode());
            String msg = e.getMessage() != null ? e.getMessage() : "Supabase error";
            return ResponseEntity.status(status).body(Map.of("message", msg));
        } catch (IllegalArgumentException e) {
            logger.error("updateCity id={} - IllegalArgumentException: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("updateCity id={} - unexpected exception", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Delete a city - requires access token in Authorization header
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCity(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String accessToken = extractAccessToken(authHeader);
            SupabaseClient authenticatedClient = supabaseClient.withAccessToken(accessToken);

            authenticatedClient.postgrest()
                    .from("cities")
                    .eq("id", id)
                    .delete();

            return ResponseEntity.noContent().build();
        } catch (SupabaseException e) {
            logger.error("deleteCity id={} - SupabaseException status={} message={}", id, e.getStatusCode(), e.getMessage(), e);
            int status = resolveHttpStatus(e.getStatusCode());
            String msg = e.getMessage() != null ? e.getMessage() : "Supabase error";
            return ResponseEntity.status(status).body(Map.of("message", msg));
        } catch (IllegalArgumentException e) {
            logger.error("deleteCity id={} - IllegalArgumentException: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("deleteCity id={} - unexpected exception", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Helper method to extract access token from Authorization header
     * Expected format: "Bearer <token>"
     */
    private String extractAccessToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        throw new IllegalArgumentException("Invalid or missing Authorization header");
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

