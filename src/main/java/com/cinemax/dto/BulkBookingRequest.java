package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

// ─────────────────────────────────────────────
// Bulk Booking DTO
// ─────────────────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BulkBookingRequest {
    @NotNull private Long showId;

    @NotEmpty
    @Size(max = 50, message = "Bulk booking max 50 tickets")
    private List<Long> seatIds;

    private String promoCode;
    private String paymentMethod;

    // Corporate booking details
    private String corporateName;
    private String corporateGst;
}
