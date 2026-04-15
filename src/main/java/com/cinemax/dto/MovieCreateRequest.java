package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MovieCreateRequest {
    @jakarta.validation.constraints.NotBlank private String title;
    private String description;
    private String director;
    private String cast;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Min(1) private Integer durationMinutes;
    @jakarta.validation.constraints.NotBlank private String language;
    private String genre;
    private String certification;
    private String posterUrl;
    private String trailerUrl;
    private java.time.LocalDateTime releaseDate;
}
