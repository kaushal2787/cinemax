package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ScreenRequest {
    @NotBlank private String name;
    @NotNull @Min(1) private Integer totalSeats;
    @NotBlank private String screenType;
    private List<SeatLayoutRequest> seatLayout;
}
