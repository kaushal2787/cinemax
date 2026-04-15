package com.cinemax.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

// ─────────────────────────────────────────────
// Theatre Onboarding DTOs (B2B)
// ─────────────────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TheatreOnboardRequest {
    @NotBlank private String name;
    @NotBlank private String city;
    @NotBlank private String state;
    @NotBlank private String country;
    @NotBlank private String address;
    @NotBlank private String pincode;
    @NotBlank @Email private String contactEmail;
    @NotBlank private String contactPhone;
    private List<ScreenRequest> screens;
}
