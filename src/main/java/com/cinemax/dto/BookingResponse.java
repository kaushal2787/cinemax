package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingResponse {
    private Long bookingId;
    private String bookingReference;
    private String status;
    private String paymentStatus;

    // Show details
    private String movieTitle;
    private String theatreName;
    private String screenName;
    private LocalDateTime showTime;
    private String city;

    // Seat details
    private List<SeatDetail> seats;

    // Pricing breakdown
    private Integer totalTickets;
    private BigDecimal baseAmount;
    private BigDecimal discountAmount;
    private String discountDescription;
    private BigDecimal convenienceFee;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;

    // Payment
    private String paymentUrl; // Redirect URL for payment gateway
    private LocalDateTime bookingExpiry; // 10 min to complete payment
}
