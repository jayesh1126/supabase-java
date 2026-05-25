package com.supabase_demo.example_service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class City {
    private Long id;
    private String name;
    private String country;

    @JsonProperty("population")
    private Integer population;

    @JsonProperty("created_at")
    private String createdAt;
}
