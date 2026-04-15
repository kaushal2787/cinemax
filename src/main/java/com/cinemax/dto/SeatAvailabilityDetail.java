package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SeatAvailabilityDetail {
    private Long seatId;
    private String seatNumber;
    private String row;
    private String category;
    private String status;
}
