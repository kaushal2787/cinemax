package com.cinemax.controller;

import com.cinemax.dto.*;
import com.cinemax.service.BookingService;
import com.cinemax.service.ShowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ─────────────────────────────────────────────
// ShowController Slice Tests
// ─────────────────────────────────────────────
@WebMvcTest(ShowController.class)
@ActiveProfiles("test")
@DisplayName("ShowController — REST layer")
class ShowControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean ShowService showService;

    @Test
    @DisplayName("GET /api/v1/shows returns 200 with theatre list")
    @WithMockUser
    void browseShowsReturns200() throws Exception {
        ShowResponse showResp = ShowResponse.builder()
            .showId(1L)
            .showTime(LocalDateTime.now().plusHours(3))
            .showPeriod("EVENING")
            .theatreName("PVR Cinemas")
            .movieTitle("Kalki 2898 AD")
            .availableSeats(80)
            .totalSeats(200)
            .isFastFilling(false)
            .isSoldOut(false)
            .silverPrice(new BigDecimal("150"))
            .goldPrice(new BigDecimal("250"))
            .applicableOffers(List.of())
            .build();

        TheatreShowsResponse theatreResp = TheatreShowsResponse.builder()
            .theatreId(10L)
            .theatreName("PVR Cinemas")
            .city("Bengaluru")
            .rating(4.3)
            .shows(List.of(showResp))
            .build();

        when(showService.browseShowsByMovieAndCity(any())).thenReturn(List.of(theatreResp));

        mockMvc.perform(get("/api/v1/shows")
                .param("city", "Bengaluru")
                .param("movieId", "1")
                .param("date", LocalDate.now().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].theatreName").value("PVR Cinemas"))
            .andExpect(jsonPath("$.data[0].shows[0].movieTitle").value("Kalki 2898 AD"))
            .andExpect(jsonPath("$.data[0].shows[0].availableSeats").value(80));
    }

    @Test
    @DisplayName("GET /api/v1/shows without required params returns 400")
    @WithMockUser
    void browseShowsMissingParamReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/shows")
                .param("movieId", "1"))   // missing city and date
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/shows is publicly accessible (no auth needed)")
    void browseShowsIsPublic() throws Exception {
        when(showService.browseShowsByMovieAndCity(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/shows")
                .param("city", "Mumbai")
                .param("movieId", "1")
                .param("date", LocalDate.now().toString()))
            .andExpect(status().isOk());
    }
}

// ─────────────────────────────────────────────
// BookingController Slice Tests
// ─────────────────────────────────────────────
@WebMvcTest(BookingController.class)
@ActiveProfiles("test")
@DisplayName("BookingController — REST layer")
class BookingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean BookingService bookingService;

    @Nested
    @DisplayName("POST /api/v1/bookings")
    class BookTickets {

        @Test
        @DisplayName("Returns 201 with booking response on success")
        @WithMockUser(roles = "CUSTOMER")
        void bookTicketsReturns201() throws Exception {
            BookingResponse response = BookingResponse.builder()
                .bookingId(1L)
                .bookingReference("CX-2024-001234")
                .status("PENDING")
                .paymentStatus("PENDING")
                .movieTitle("Kalki 2898 AD")
                .theatreName("PVR Cinemas")
                .totalTickets(2)
                .baseAmount(new BigDecimal("400.00"))
                .discountAmount(new BigDecimal("0.00"))
                .convenienceFee(new BigDecimal("8.00"))
                .taxAmount(new BigDecimal("73.44"))
                .totalAmount(new BigDecimal("481.44"))
                .paymentUrl("https://checkout.razorpay.com/pay/CX-2024-001234")
                .bookingExpiry(LocalDateTime.now().plusMinutes(10))
                .build();

            when(bookingService.bookTickets(any(), any())).thenReturn(response);

            BookingRequest request = BookingRequest.builder()
                .showId(1L)
                .seatIds(List.of(101L, 102L))
                .paymentMethod("UPI")
                .build();

            mockMvc.perform(post("/api/v1/bookings")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookingReference").value("CX-2024-001234"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(481.44));
        }

        @Test
        @DisplayName("Returns 401 when unauthenticated")
        void bookTicketsRequiresAuth() throws Exception {
            BookingRequest request = BookingRequest.builder()
                .showId(1L).seatIds(List.of(1L)).build();

            mockMvc.perform(post("/api/v1/bookings")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Returns 400 when seatIds is empty")
        @WithMockUser(roles = "CUSTOMER")
        void bookTicketsEmptySeatsReturns400() throws Exception {
            BookingRequest request = BookingRequest.builder()
                .showId(1L)
                .seatIds(List.of())  // empty — fails @NotEmpty
                .build();

            mockMvc.perform(post("/api/v1/bookings")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/bookings/{reference}")
    class CancelBooking {

        @Test
        @DisplayName("Returns 200 on successful cancellation")
        @WithMockUser
        void cancelBookingReturns200() throws Exception {
            mockMvc.perform(delete("/api/v1/bookings/CX-2024-001234")
                    .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(
                    org.hamcrest.Matchers.containsString("Refund")));
        }
    }
}
