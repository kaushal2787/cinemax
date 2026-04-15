package com.cinemax.controller;

import com.cinemax.dto.*;
import com.cinemax.service.BookingService;
import com.cinemax.service.ShowManagementService;
import com.cinemax.service.ShowService;
import com.cinemax.service.TheatreService;
import com.cinemax.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.cinemax.model.*;
import java.util.List;

// ─────────────────────────────────────────────
// Theatre Management Controller (B2B)
// ─────────────────────────────────────────────
@RestController
@RequestMapping("/api/v1/partner/theatres")
@Tag(name = "Theatre Management", description = "B2B Theatre onboarding and management")
@RequiredArgsConstructor
@PreAuthorize("hasRole('THEATRE_PARTNER')")
public class TheatreManagementController {

    private final TheatreService theatreService;

    @PostMapping
    @Operation(summary = "Onboard a new theatre")
    public ResponseEntity<ApiResponse<TheatreResponse>> onboardTheatre(
            @Valid @RequestBody TheatreOnboardRequest request,
            @AuthenticationPrincipal Long partnerId) {
        TheatreResponse theatre = theatreService.onboardTheatre(request, partnerId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(theatre, "Theatre onboarded. Pending approval."));
    }

    @GetMapping
    @Operation(summary = "List all theatres for the partner")
    public ResponseEntity<ApiResponse<List<TheatreResponse>>> listTheatres(
            @AuthenticationPrincipal Long partnerId) {
        return ResponseEntity.ok(ApiResponse.success(theatreService.getTheatresByPartner(partnerId)));
    }
}
