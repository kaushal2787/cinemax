package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PriceCalculationResponse {
    private List<SeatDetail> seats;
    private BigDecimal baseAmount;
    private BigDecimal discountAmount;
    private String discountBreakdown;
    private BigDecimal convenienceFee;
    private BigDecimal gst;
    private BigDecimal totalAmount;
    private List<OfferSummary> appliedOffers;
}
