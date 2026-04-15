package com.cinemax.service;

import com.cinemax.dto.*;
import com.cinemax.exception.BusinessException;
import com.cinemax.exception.ResourceNotFoundException;
import com.cinemax.exception.SeatUnavailableException;
import com.cinemax.model.*;
import com.cinemax.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// ─────────────────────────────────────────────
// ShowService Tests
// ─────────────────────────────────────────────
@ExtendWith(MockitoExtension.class)
@DisplayName("ShowService — browse shows")
class ShowServiceTest {

    @Mock private ShowRepository         showRepository;
    @Mock private OfferService           offerService;
    @Mock private SeatAvailabilityService seatAvailabilityService;

    @InjectMocks private ShowService showService;

    @Test
    @DisplayName("browseShowsByMovieAndCity returns grouped theatre responses")
    void browseShowsGroupedByTheatre() {
        ShowSearchRequest request = ShowSearchRequest.builder()
            .city("Mumbai").movieId(1L).date(LocalDate.now()).build();

        ShowProjection proj = mockProjection(10L, "PVR Juhu", "Mumbai", 100, 200);
        when(showRepository.findShowsGroupedByTheatre(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(proj));
        when(offerService.getApplicableOffers(any(), any(), any())).thenReturn(List.of());

        List<TheatreShowsResponse> result = showService.browseShowsByMovieAndCity(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTheatreName()).isEqualTo("PVR Juhu");
        assertThat(result.get(0).getShows()).hasSize(1);
    }

    @Test
    @DisplayName("isFastFilling flagged when < 20% seats remain")
    void fastFillingFlagSetWhenFewSeatsLeft() {
        ShowSearchRequest request = ShowSearchRequest.builder()
            .city("Delhi").movieId(2L).date(LocalDate.now()).build();

        ShowProjection proj = mockProjection(20L, "Cinepolis", "Delhi", 19, 100); // 19% left
        when(showRepository.findShowsGroupedByTheatre(any(), any(), any(), any(), any(), any()))
            .thenReturn(List.of(proj));
        when(offerService.getApplicableOffers(any(), any(), any())).thenReturn(List.of());

        List<TheatreShowsResponse> result = showService.browseShowsByMovieAndCity(request);

        assertThat(result.get(0).getShows().get(0).getIsFastFilling()).isTrue();
    }

    @Test
    @DisplayName("determineShowPeriod correctly classifies time slots")
    void showPeriodClassification() {
        assertThat(showService.determineShowPeriod(java.time.LocalTime.of(9, 0))).isEqualTo("MORNING");
        assertThat(showService.determineShowPeriod(java.time.LocalTime.of(12, 0))).isEqualTo("AFTERNOON");
        assertThat(showService.determineShowPeriod(java.time.LocalTime.of(16, 59))).isEqualTo("AFTERNOON");
        assertThat(showService.determineShowPeriod(java.time.LocalTime.of(17, 0))).isEqualTo("EVENING");
        assertThat(showService.determineShowPeriod(java.time.LocalTime.of(20, 0))).isEqualTo("NIGHT");
    }

    // ── helpers ──────────────────────────────
    private ShowProjection mockProjection(Long theatreId, String theatreName,
                                          String city, int available, int total) {
        ShowProjection p = mock(ShowProjection.class);
        when(p.getShowId()).thenReturn(1L);
        when(p.getTheatreId()).thenReturn(theatreId);
        when(p.getTheatreName()).thenReturn(theatreName);
        when(p.getTheatreAddress()).thenReturn("123 Main St");
        when(p.getCity()).thenReturn(city);
        when(p.getTheatreRating()).thenReturn(4.2);
        when(p.getShowTime()).thenReturn(LocalDateTime.now().withHour(18));
        when(p.getStatus()).thenReturn(ShowStatus.SCHEDULED);
        when(p.getScreenName()).thenReturn("Screen 1");
        when(p.getScreenType()).thenReturn("STANDARD");
        when(p.getMovieId()).thenReturn(1L);
        when(p.getMovieTitle()).thenReturn("Test Movie");
        when(p.getLanguage()).thenReturn("English");
        when(p.getCertification()).thenReturn("UA");
        when(p.getAvailableSeats()).thenReturn(available);
        when(p.getTotalSeats()).thenReturn(total);
        when(p.getSilverPrice()).thenReturn(new BigDecimal("150"));
        when(p.getGoldPrice()).thenReturn(new BigDecimal("250"));
        when(p.getPlatinumPrice()).thenReturn(new BigDecimal("400"));
        when(p.getReclinePrice()).thenReturn(new BigDecimal("600"));
        return p;
    }
}

// ─────────────────────────────────────────────
// BookingService Tests
// ─────────────────────────────────────────────
@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService — booking and cancellation")
class BookingServiceTest {

    @Mock private ShowRepository     showRepository;
    @Mock private ShowSeatRepository showSeatRepository;
    @Mock private BookingRepository  bookingRepository;
    @Mock private UserRepository     userRepository;
    @Mock private OfferService       offerService;
    @Mock private PaymentService     paymentService;

    @InjectMocks private BookingService bookingService;

    @Test
    @DisplayName("bookTickets throws when show not found")
    void throwsWhenShowNotFound() {
        when(showRepository.findById(anyLong())).thenReturn(Optional.empty());

        BookingRequest req = BookingRequest.builder()
            .showId(999L).seatIds(List.of(1L)).build();

        assertThatThrownBy(() -> bookingService.bookTickets(req, 1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Show not found");
    }

    @Test
    @DisplayName("bookTickets throws when show is cancelled")
    void throwsWhenShowCancelled() {
        Show show = mockShow(ShowStatus.CANCELLED, LocalDateTime.now().plusHours(2));
        when(showRepository.findById(anyLong())).thenReturn(Optional.of(show));

        BookingRequest req = BookingRequest.builder()
            .showId(1L).seatIds(List.of(1L)).build();

        assertThatThrownBy(() -> bookingService.bookTickets(req, 1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("not available for booking");
    }

    @Test
    @DisplayName("bookTickets throws when show time has passed")
    void throwsForPastShow() {
        Show show = mockShow(ShowStatus.SCHEDULED, LocalDateTime.now().minusHours(1));
        when(showRepository.findById(anyLong())).thenReturn(Optional.of(show));

        BookingRequest req = BookingRequest.builder()
            .showId(1L).seatIds(List.of(1L)).build();

        assertThatThrownBy(() -> bookingService.bookTickets(req, 1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("past shows");
    }

    @Test
    @DisplayName("lockSeats throws SeatUnavailableException when seat is LOCKED")
    void throwsWhenSeatAlreadyLocked() {
        Show show = mockShow(ShowStatus.SCHEDULED, LocalDateTime.now().plusHours(3));
        when(showRepository.findById(anyLong())).thenReturn(Optional.of(show));

        ShowSeat lockedSeat = mockShowSeat(1L, "A1", SeatStatus.LOCKED);
        when(showSeatRepository.findByShowIdAndSeatIds(anyLong(), anyList()))
            .thenReturn(List.of(lockedSeat));

        BookingRequest req = BookingRequest.builder()
            .showId(1L).seatIds(List.of(1L)).build();

        assertThatThrownBy(() -> bookingService.bookTickets(req, 1L))
            .isInstanceOf(SeatUnavailableException.class);
    }

    @Test
    @DisplayName("cancelBooking throws when booking is PENDING (not yet confirmed)")
    void throwsWhenCancellingPendingBooking() {
        Booking booking = mockBooking(BookingStatus.PENDING);
        when(bookingRepository.findByBookingReferenceAndUserId(any(), anyLong()))
            .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking("CX-2024-001", 1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Only confirmed bookings");
    }

    @Test
    @DisplayName("cancelBooking releases seats and triggers refund")
    void cancelBookingReleasesSeatsAndRefunds() {
        ShowSeat seat   = mockShowSeat(1L, "A1", SeatStatus.BOOKED);
        Booking booking = mockBooking(BookingStatus.CONFIRMED);
        BookingItem item = mock(BookingItem.class);
        when(item.getShowSeat()).thenReturn(seat);
        when(booking.getBookingItems()).thenReturn(List.of(item));

        when(bookingRepository.findByBookingReferenceAndUserId(any(), anyLong()))
            .thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenReturn(booking);
        when(showSeatRepository.save(any())).thenReturn(seat);

        bookingService.cancelBooking("CX-2024-001", 1L);

        verify(showSeatRepository, times(1)).save(seat);
        verify(paymentService, times(1)).initiateRefund(booking);
        verify(booking).setStatus(BookingStatus.CANCELLED);
    }

    // ── helpers ──────────────────────────────
    private Show mockShow(ShowStatus status, LocalDateTime showTime) {
        Show show = mock(Show.class);
        when(show.getStatus()).thenReturn(status);
        when(show.getShowTime()).thenReturn(showTime);
        return show;
    }

    private ShowSeat mockShowSeat(Long id, String seatNum, SeatStatus status) {
        ShowSeat ss   = mock(ShowSeat.class);
        Seat seat     = mock(Seat.class);
        when(ss.getSeat()).thenReturn(seat);
        when(ss.getStatus()).thenReturn(status);
        when(seat.getId()).thenReturn(id);
        when(seat.getSeatNumber()).thenReturn(seatNum);
        return ss;
    }

    private Booking mockBooking(BookingStatus status) {
        Booking b = mock(Booking.class);
        when(b.getStatus()).thenReturn(status);
        when(b.getBookingReference()).thenReturn("CX-2024-001");
        when(b.getTotalAmount()).thenReturn(BigDecimal.valueOf(500));
        return b;
    }
}

// ─────────────────────────────────────────────
// ShowManagementService Tests
// ─────────────────────────────────────────────
@ExtendWith(MockitoExtension.class)
@DisplayName("ShowManagementService — B2B show CRUD")
class ShowManagementServiceTest {

    @Mock private ShowRepository     showRepository;
    @Mock private ScreenRepository   screenRepository;
    @Mock private MovieRepository    movieRepository;
    @Mock private ShowSeatRepository showSeatRepository;

    @InjectMocks private ShowManagementService showManagementService;

    @Test
    @DisplayName("createShow throws when screen not found")
    void throwsWhenScreenNotFound() {
        when(screenRepository.findById(anyLong())).thenReturn(Optional.empty());

        CreateShowRequest req = CreateShowRequest.builder()
            .movieId(1L).screenId(999L)
            .showTime(LocalDateTime.now().plusDays(1))
            .silverPrice(BigDecimal.valueOf(150))
            .goldPrice(BigDecimal.valueOf(250))
            .platinumPrice(BigDecimal.valueOf(400))
            .build();

        assertThatThrownBy(() -> showManagementService.createShow(req, 1L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Screen not found");
    }

    @Test
    @DisplayName("deleteShow throws when show is RUNNING")
    void throwsWhenDeletingRunningShow() {
        Show show = mock(Show.class);
        when(show.getStatus()).thenReturn(ShowStatus.RUNNING);
        Screen screen = mock(Screen.class);
        Theatre theatre = mock(Theatre.class);
        TheatrePartner partner = mock(TheatrePartner.class);
        when(show.getScreen()).thenReturn(screen);
        when(screen.getTheatre()).thenReturn(theatre);
        when(theatre.getPartner()).thenReturn(partner);
        when(partner.getId()).thenReturn(1L);
        when(showRepository.findById(anyLong())).thenReturn(Optional.of(show));

        assertThatThrownBy(() -> showManagementService.deleteShow(1L, 1L))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Cannot delete a running show");
    }

    @Test
    @DisplayName("updateShow throws when show is COMPLETED")
    void throwsWhenUpdatingCompletedShow() {
        Show show = mock(Show.class);
        when(show.getStatus()).thenReturn(ShowStatus.COMPLETED);
        Screen screen = mock(Screen.class);
        Theatre theatre = mock(Theatre.class);
        TheatrePartner partner = mock(TheatrePartner.class);
        when(show.getScreen()).thenReturn(screen);
        when(screen.getTheatre()).thenReturn(theatre);
        when(theatre.getPartner()).thenReturn(partner);
        when(partner.getId()).thenReturn(1L);
        when(showRepository.findById(anyLong())).thenReturn(Optional.of(show));

        UpdateShowRequest req = UpdateShowRequest.builder()
            .silverPrice(BigDecimal.valueOf(200)).build();

        assertThatThrownBy(() -> showManagementService.updateShow(1L, req, 1L))
            .isInstanceOf(BusinessException.class);
    }
}
