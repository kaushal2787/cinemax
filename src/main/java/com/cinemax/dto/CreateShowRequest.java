package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ─────────────────────────────────────────────
// Show Management DTOs (B2B - Theatre Partner)
// ─────────────────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateShowRequest {
    @NotNull private Long movieId;
    @NotNull private Long screenId;

    @NotNull private LocalDateTime showTime;

    @NotNull @DecimalMin("0.0") private BigDecimal silverPrice;
    @NotNull @DecimalMin("0.0") private BigDecimal goldPrice;
    @NotNull @DecimalMin("0.0") private BigDecimal platinumPrice;
    private BigDecimal reclinePrice;
}
