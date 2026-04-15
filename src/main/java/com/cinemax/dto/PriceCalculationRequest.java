package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

// ─────────────────────────────────────────────
// Offer/Discount DTOs
// ─────────────────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PriceCalculationRequest {
    @NotNull private Long showId;
    @NotEmpty private List<Long> seatIds;
    private String promoCode;
}
