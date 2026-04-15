package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TheatreResponse {
    private Long id;
    private String name;
    private String city;
    private String address;
    private String status;
    private List<ScreenRequest> screens;
}
