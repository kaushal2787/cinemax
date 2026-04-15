package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OfferSummary {
    private String offerCode;
    private String description;
    private String offerType;
    private BigDecimal discountPercentage;
}
