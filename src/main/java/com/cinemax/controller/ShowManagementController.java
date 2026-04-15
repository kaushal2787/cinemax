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

// ─────────────────────────────────────────────
// Show Management Controller (B2B)
// ─────────────────────────────────────────────
@RestController
@RequestMapping("/api/v1/partner/shows")
@Tag(name = "Show Management", description = "B2B Show CRUD for theatre partners")
@RequiredArgsConstructor
@PreAuthorize("hasRole('THEATRE_PARTNER')")
public class ShowManagementController {

    private final ShowManagementService showManagementService;

    @PostMapping
    @Operation(summary = "Create a new show")
    public ResponseEntity<ApiResponse<ShowResponse>> createShow(
            @Valid @RequestBody CreateShowRequest request,
            @AuthenticationPrincipal Long partnerId) {
        ShowResponse show = showManagementService.createShow(request, partnerId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(show, "Show created successfully"));
    }

    @PutMapping("/{showId}")
    @Operation(summary = "Update show details")
    public ResponseEntity<ApiResponse<ShowResponse>> updateShow(
            @PathVariable Long showId,
            @Valid @RequestBody UpdateShowRequest request,
            @AuthenticationPrincipal Long partnerId) {
        ShowResponse show = showManagementService.updateShow(showId, request, partnerId);
        return ResponseEntity.ok(ApiResponse.success(show, "Show updated successfully"));
    }

    @DeleteMapping("/{showId}")
    @Operation(summary = "Cancel a show")
    public ResponseEntity<ApiResponse<Void>> deleteShow(
            @PathVariable Long showId,
            @AuthenticationPrincipal Long partnerId) {
        showManagementService.deleteShow(showId, partnerId);
        return ResponseEntity.ok(ApiResponse.success(null, "Show cancelled successfully"));
    }

    @PostMapping("/{showId}/inventory")
    @Operation(summary = "Allocate/update seat inventory for a show")
    public ResponseEntity<ApiResponse<Void>> updateSeatInventory(
            @PathVariable Long showId,
            @AuthenticationPrincipal Long partnerId) {
        showManagementService.reallocateSeatInventory(showId, partnerId);
        return ResponseEntity.ok(ApiResponse.success(null, "Seat inventory updated"));
    }
}
