package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TheatreShowsResponse {
    private Long theatreId;
    private String theatreName;
    private String address;
    private String city;
    private Double rating;
    private List<ShowResponse> shows;
}
