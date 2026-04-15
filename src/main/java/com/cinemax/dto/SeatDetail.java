package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SeatDetail {
    private Long seatId;
    private String seatNumber;
    private String row;
    private String category;
    private BigDecimal price;
    private BigDecimal discount;
}
