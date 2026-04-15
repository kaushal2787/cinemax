package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SeatLayoutRequest {
    @NotBlank private String row;
    @NotNull private Integer seatsInRow;
    @NotBlank private String category; // SILVER, GOLD, PLATINUM
}
