package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

// ─────────────────────────────────────────────
// Movie DTOs
// ─────────────────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MovieSummaryResponse {
    private Long id;
    private String title;
    private String description;
    private String director;
    private String cast;
    private Integer durationMinutes;
    private String language;
    private String genre;
    private String certification;
    private String posterUrl;
    private String trailerUrl;
    private java.time.LocalDateTime releaseDate;
    private String status;
}
