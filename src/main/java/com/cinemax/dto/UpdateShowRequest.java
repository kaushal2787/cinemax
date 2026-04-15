package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UpdateShowRequest {
    private LocalDateTime showTime;
    private BigDecimal silverPrice;
    private BigDecimal goldPrice;
    private BigDecimal platinumPrice;
    private BigDecimal reclinePrice;
    private String status; // SCHEDULED, CANCELLED
}
