package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

// ─────────────────────────────────────────────
// Booking DTOs (WRITE SCENARIO)
// ─────────────────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingRequest {
    @NotNull private Long showId;

    @NotEmpty(message = "At least one seat must be selected")
    @Size(max = 10, message = "Cannot book more than 10 tickets at once")
    private List<Long> seatIds;

    private String promoCode;
    private String paymentMethod; // UPI, CARD, NETBANKING, WALLET
}
