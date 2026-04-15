package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

// ─────────────────────────────────────────────
// Additional response DTOs
// ─────────────────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SeatLayoutResponse {
    private Long showId;
    private List<SeatAvailabilityDetail> seats;
}
