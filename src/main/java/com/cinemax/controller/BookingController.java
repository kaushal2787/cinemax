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
// Booking Controller (B2C)
// ─────────────────────────────────────────────
@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Ticket booking and cancellation")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Book movie tickets")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> bookTickets(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal Long userId) {
        BookingResponse response = bookingService.bookTickets(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Booking created. Complete payment within 10 minutes."));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk book movie tickets (up to 50)")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingResponse>> bulkBookTickets(
            @Valid @RequestBody BulkBookingRequest request,
            @AuthenticationPrincipal Long userId) {
        BookingResponse response = bookingService.bulkBookTickets(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{reference}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BookingResponse>> getBooking(
            @PathVariable String reference,
            @AuthenticationPrincipal Long userId) {
        BookingResponse booking = bookingService.getBooking(reference, userId);
        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    @DeleteMapping("/{reference}")
    @Operation(summary = "Cancel a booking")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @PathVariable String reference,
            @AuthenticationPrincipal Long userId) {
        bookingService.cancelBooking(reference, userId);
        return ResponseEntity.ok(ApiResponse.success(null,
            "Booking cancelled. Refund will be processed in 5-7 business days."));
    }

    @PostMapping("/cancel/bulk")
    @Operation(summary = "Bulk cancel bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> bulkCancelBookings(
            @Valid @RequestBody BulkCancellationRequest request,
            @AuthenticationPrincipal Long userId) {
        bookingService.bulkCancelBookings(request, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Bookings cancelled successfully"));
    }
}
