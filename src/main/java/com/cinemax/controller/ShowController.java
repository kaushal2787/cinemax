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
// Show Browse Controller (B2C)
// ─────────────────────────────────────────────
@RestController
@RequestMapping("/api/v1/shows")
@Tag(name = "Shows", description = "Browse shows and seat availability")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @GetMapping
    @Operation(summary = "Browse shows by city, movie and date")
    public ResponseEntity<ApiResponse<List<TheatreShowsResponse>>> browseShows(
            @Valid ShowSearchRequest request) {
        List<TheatreShowsResponse> shows = showService.browseShowsByMovieAndCity(request);
        return ResponseEntity.ok(ApiResponse.success(shows,
            String.format("Found %d theatres showing this movie", shows.size())));
    }

    @GetMapping("/{showId}/seats")
    @Operation(summary = "Get seat layout and availability for a show")
    public ResponseEntity<ApiResponse<SeatLayoutResponse>> getSeatLayout(
            @PathVariable Long showId) {
        SeatLayoutResponse layout = showService.getSeatLayout(showId);
        return ResponseEntity.ok(ApiResponse.success(layout));
    }

    @PostMapping("/price")
    @Operation(summary = "Calculate price with applicable discounts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PriceCalculationResponse>> calculatePrice(
            @Valid @RequestBody PriceCalculationRequest request) {
        PriceCalculationResponse pricing = showService.calculatePrice(request);
        return ResponseEntity.ok(ApiResponse.success(pricing));
    }
}
