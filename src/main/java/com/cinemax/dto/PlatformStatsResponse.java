package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

// ─────────────────────────────────────────────
// Admin DTOs
// ─────────────────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PlatformStatsResponse {
    private long totalUsers;
    private long totalTheatres;
    private long totalMovies;
    private Long totalBookingsToday;
    private Long totalRevenueToday;
}
