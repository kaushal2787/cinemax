package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

// ─────────────────────────────────────────────
// Show Browse DTOs (READ SCENARIO)
// ─────────────────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ShowSearchRequest {
    @NotBlank(message = "City is required")
    private String city;

    @NotNull(message = "Movie ID is required")
    private Long movieId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private String language;
    private String screenType;
    private String sortBy; // price, time, rating
}
