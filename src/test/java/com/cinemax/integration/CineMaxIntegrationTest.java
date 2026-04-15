package com.cinemax.integration;

import com.cinemax.dto.*;
import com.cinemax.model.*;
import com.cinemax.repository.*;
import com.cinemax.service.BookingService;
import com.cinemax.service.OfferService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests — full Spring context with H2 in-memory database.
 * Redis is not available in test environment so PaymentService is mocked.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("CineMax Integration Tests")
@Transactional
class CineMaxIntegrationTest {

    @Autowired UserRepository     userRepository;
    @Autowired MovieRepository    movieRepository;
    @Autowired TheatreRepository  theatreRepository;
    @Autowired ScreenRepository   screenRepository;
    @Autowired ShowRepository     showRepository;
    @Autowired ShowSeatRepository showSeatRepository;
    @Autowired BookingRepository  bookingRepository;
    @Autowired com.cinemax.repository.SeatRepository seatRepository;
    @Autowired com.cinemax.repository.TheatrePartnerRepository partnerRepository;
    @Autowired BookingService     bookingService;
    @Autowired OfferService       offerService;

    @MockBean com.cinemax.service.PaymentService paymentService;

    private User   testUser;
    private Movie  testMovie;
    private Show   testShow;
    private Screen testScreen;

    @BeforeEach
    void setUp() {
        when(paymentService.initiatePayment(any())).thenReturn("https://checkout.razorpay.com/test");

        // Create user
        testUser = userRepository.save(User.builder()
            .email("test@cinemax.com")
            .passwordHash("$2a$12$hashedpassword")
            .firstName("Test").lastName("User")
            .role(UserRole.CUSTOMER)
            .isActive(true).isVerified(true)
            .build());

        // Create movie
        testMovie = movieRepository.save(Movie.builder()
            .title("Test Movie").language("English")
            .durationMinutes(150).genre("Action")
            .certification("UA").status(MovieStatus.NOW_SHOWING)
            .build());

        // Create theatre → screen → seats
        TheatrePartner partner = partnerRepository.save(TheatrePartner.builder()
            .businessName("Test Partner").email("partner@test.com")
            .status(PartnerStatus.ACTIVE).build());

        Theatre theatre = theatreRepository.save(Theatre.builder()
            .name("Test Theatre").city("Bengaluru")
            .partner(partner)
            .status(TheatreStatus.ACTIVE).rating(4.0).build());

        testScreen = screenRepository.save(Screen.builder()
            .theatre(theatre).name("Screen 1")
            .totalSeats(100).screenType(ScreenType.STANDARD)
            .build());

        // Create show
        testShow = showRepository.save(Show.builder()
            .movie(testMovie).screen(testScreen)
            .showTime(LocalDateTime.now().plusHours(3))
            .showEndTime(LocalDateTime.now().plusHours(5).plusMinutes(30))
            .status(ShowStatus.SCHEDULED)
            .silverPrice(new BigDecimal("150"))
            .goldPrice(new BigDecimal("250"))
            .platinumPrice(new BigDecimal("400"))
            .totalSeats(100).availableSeats(100)
            .build());

        // Allocate seats (must save Seat entity before ShowSeat FK)
        for (int i = 1; i <= 6; i++) {
            Seat seat = seatRepository.save(Seat.builder()
                .screen(testScreen)
                .seatNumber("A" + i).row("A")
                .seatIndex(i).category(SeatCategory.GOLD)
                .build());
            showSeatRepository.save(ShowSeat.builder()
                .show(testShow).seat(seat)
                .status(SeatStatus.AVAILABLE).version(0L)
                .build());
        }
    }

    // ─────────────────────────────────────────
    @Nested
    @DisplayName("Booking flow end-to-end")
    class BookingFlowTest {

        @Test
        @DisplayName("Booking 2 seats creates PENDING booking with correct amounts")
        void bookTwoSeats() {
            List<Long> seatIds = showSeatRepository.findByShowId(testShow.getId())
                .stream().limit(2).map(ss -> ss.getSeat().getId()).toList();

            BookingRequest req = BookingRequest.builder()
                .showId(testShow.getId()).seatIds(seatIds).build();

            BookingResponse response = bookingService.bookTickets(req, testUser.getId());

            assertThat(response.getBookingReference()).startsWith("CX-");
            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(response.getTotalTickets()).isEqualTo(2);
            assertThat(response.getBaseAmount()).isEqualByComparingTo("500.00"); // 2 × 250 GOLD
            assertThat(response.getDiscountAmount()).isEqualByComparingTo("0.00"); // no 3rd ticket
            assertThat(response.getPaymentUrl()).contains("razorpay");

            // Verify seats are now LOCKED
            List<ShowSeat> locked = showSeatRepository.findByShowIdAndSeatIds(
                testShow.getId(), seatIds);
            assertThat(locked).allMatch(ss -> ss.getStatus() == SeatStatus.LOCKED);
        }

        @Test
        @DisplayName("Booking 3 seats applies 50% discount on 3rd ticket")
        void bookThreeSeatsAppliesDiscount() {
            List<Long> seatIds = showSeatRepository.findByShowId(testShow.getId())
                .stream().limit(3).map(ss -> ss.getSeat().getId()).toList();

            BookingRequest req = BookingRequest.builder()
                .showId(testShow.getId()).seatIds(seatIds).build();

            BookingResponse response = bookingService.bookTickets(req, testUser.getId());

            // 3 × 250 = 750 base; ticket 3 gets 50% = 125 discount
            assertThat(response.getBaseAmount()).isEqualByComparingTo("750.00");
            assertThat(response.getDiscountAmount()).isEqualByComparingTo("125.00");
            assertThat(response.getDiscountDescription()).contains("50%");
        }

        @Test
        @DisplayName("confirmBooking transitions PENDING → CONFIRMED and seats → BOOKED")
        void confirmBookingAfterPayment() {
            List<Long> seatIds = showSeatRepository.findByShowId(testShow.getId())
                .stream().limit(2).map(ss -> ss.getSeat().getId()).toList();

            BookingRequest req = BookingRequest.builder()
                .showId(testShow.getId()).seatIds(seatIds).build();
            BookingResponse booking = bookingService.bookTickets(req, testUser.getId());

            // Simulate webhook confirmation
            bookingService.confirmBooking(booking.getBookingReference(), "pay_test123");

            Booking confirmed = bookingRepository
                .findByBookingReference(booking.getBookingReference()).orElseThrow();

            assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(confirmed.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(confirmed.getPaymentId()).isEqualTo("pay_test123");

            // All items should be BOOKED
            confirmed.getBookingItems().forEach(item ->
                assertThat(item.getShowSeat().getStatus()).isEqualTo(SeatStatus.BOOKED));
        }

        @Test
        @DisplayName("Cannot book seat already LOCKED by another user")
        void cannotBookLockedSeat() {
            List<ShowSeat> seats = showSeatRepository.findByShowId(testShow.getId());
            ShowSeat seatToLock = seats.get(0);
            seatToLock.setStatus(SeatStatus.LOCKED);
            seatToLock.setLockedByUserId("99"); // locked by another user
            showSeatRepository.save(seatToLock);

            List<Long> seatIds = List.of(seatToLock.getSeat().getId());
            BookingRequest req = BookingRequest.builder()
                .showId(testShow.getId()).seatIds(seatIds).build();

            assertThatThrownBy(() -> bookingService.bookTickets(req, testUser.getId()))
                .isInstanceOf(com.cinemax.exception.SeatUnavailableException.class);
        }

        @Test
        @DisplayName("Idempotent confirmBooking — double webhook does not throw")
        void idempotentConfirm() {
            List<Long> seatIds = showSeatRepository.findByShowId(testShow.getId())
                .stream().limit(1).map(ss -> ss.getSeat().getId()).toList();

            BookingResponse booking = bookingService.bookTickets(
                BookingRequest.builder().showId(testShow.getId()).seatIds(seatIds).build(),
                testUser.getId());

            bookingService.confirmBooking(booking.getBookingReference(), "pay_001");
            // Second call with same paymentId should be idempotent, not throw
            assertThatNoException().isThrownBy(() ->
                bookingService.confirmBooking(booking.getBookingReference(), "pay_001"));
        }
    }

    // ─────────────────────────────────────────
    @Nested
    @DisplayName("Offer/discount engine")
    class DiscountEngineTest {

        @Test
        @DisplayName("Afternoon show (14:00) applies 20% discount to all tickets")
        void afternoonDiscountApplied() {
            testShow.setShowTime(LocalDateTime.now().withHour(14).withMinute(0));
            showRepository.save(testShow);

            List<Long> seatIds = showSeatRepository.findByShowId(testShow.getId())
                .stream().limit(2).map(ss -> ss.getSeat().getId()).toList();

            BookingRequest req = BookingRequest.builder()
                .showId(testShow.getId()).seatIds(seatIds).build();
            BookingResponse response = bookingService.bookTickets(req, testUser.getId());

            // 2 × 250 = 500; 20% off both = 100 discount
            assertThat(response.getDiscountAmount()).isEqualByComparingTo("100.00");
        }
    }
}
