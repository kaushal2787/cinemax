package com.cinemax.service;

import com.cinemax.exception.*;

import com.cinemax.dto.*;
import com.cinemax.exception.BusinessException;
import com.cinemax.exception.ResourceNotFoundException;
import com.cinemax.exception.SeatUnavailableException;
import com.cinemax.exception.UnauthorizedException;
import com.cinemax.model.*;
import com.cinemax.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

// ─────────────────────────────────────────────
// BookingService — WRITE SCENARIO
// ─────────────────────────────────────────────
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final ShowRepository     showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final BookingRepository  bookingRepository;
    private final UserRepository     userRepository;
    private final OfferService       offerService;
    private final PaymentService     paymentService;

    private static final int SEAT_LOCK_MINUTES = 10;

    /**
     * WRITE: Book tickets — lock seats → calculate price → create PENDING booking → initiate payment.
     */
    @Transactional
    public BookingResponse bookTickets(BookingRequest request, Long userId) {
        log.info("Booking: showId={} seats={} userId={}", request.getShowId(), request.getSeatIds(), userId);

        Show show = showRepository.findById(request.getShowId())
            .orElseThrow(() -> new ResourceNotFoundException("Show not found"));
        validateShowBookable(show);

        List<ShowSeat> lockedSeats = lockSeats(request.getSeatIds(), request.getShowId(), userId);

        PriceCalculationRequest priceReq = PriceCalculationRequest.builder()
            .showId(request.getShowId())
            .seatIds(request.getSeatIds())
            .promoCode(request.getPromoCode())
            .build();
        PriceCalculationResponse pricing = offerService.calculatePrice(priceReq, show.getShowTime());

        String bookingRef = generateBookingReference();
        Booking booking   = createBooking(bookingRef, userId, show, lockedSeats, pricing);
        String paymentUrl = paymentService.initiatePayment(booking);

        return BookingResponse.builder()
            .bookingId(booking.getId())
            .bookingReference(bookingRef)
            .status("PENDING")
            .paymentStatus("PENDING")
            .movieTitle(show.getMovie().getTitle())
            .theatreName(show.getScreen().getTheatre().getName())
            .screenName(show.getScreen().getName())
            .showTime(show.getShowTime())
            .city(show.getScreen().getTheatre().getCity())
            .seats(pricing.getSeats())
            .totalTickets(request.getSeatIds().size())
            .baseAmount(pricing.getBaseAmount())
            .discountAmount(pricing.getDiscountAmount())
            .discountDescription(pricing.getDiscountBreakdown())
            .convenienceFee(pricing.getConvenienceFee())
            .taxAmount(pricing.getGst())
            .totalAmount(pricing.getTotalAmount())
            .paymentUrl(paymentUrl)
            .bookingExpiry(LocalDateTime.now().plusMinutes(SEAT_LOCK_MINUTES))
            .build();
    }

    @Transactional
    public BookingResponse bulkBookTickets(BulkBookingRequest request, Long userId) {
        BookingRequest br = BookingRequest.builder()
            .showId(request.getShowId())
            .seatIds(request.getSeatIds())
            .promoCode(request.getPromoCode())
            .paymentMethod(request.getPaymentMethod())
            .build();
        return bookTickets(br, userId);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(String bookingReference, Long userId) {
        Booking booking = bookingRepository.findByBookingReferenceAndUserId(bookingReference, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return mapToBookingResponse(booking);
    }

    @Transactional
    public void cancelBooking(String bookingReference, Long userId) {
        Booking booking = bookingRepository.findByBookingReferenceAndUserId(bookingReference, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessException("Only confirmed bookings can be cancelled");
        }
        booking.getBookingItems().forEach(item -> {
            ShowSeat seat = item.getShowSeat();
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setLockedByUserId(null);
            showSeatRepository.save(seat);
        });
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        paymentService.initiateRefund(booking);
        log.info("Booking {} cancelled for user {}", bookingReference, userId);
    }

    @Transactional
    public void bulkCancelBookings(BulkCancellationRequest request, Long userId) {
        request.getBookingReferences().forEach(ref -> cancelBooking(ref, userId));
    }

    /**
     * Confirms a booking after successful payment webhook from Razorpay.
     * Transitions: PENDING → CONFIRMED, seats: LOCKED → BOOKED
     *
     * @param bookingReference the booking reference (= Razorpay receipt)
     * @param paymentId        the Razorpay payment_id from the webhook
     */
    @Transactional
    public void confirmBooking(String bookingReference, String paymentId) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Booking not found: " + bookingReference));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.warn("Booking {} already confirmed — idempotent webhook ignored", bookingReference);
            return;
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException(
                "Cannot confirm booking in status: " + booking.getStatus());
        }

        // Mark all locked seats as BOOKED
        booking.getBookingItems().forEach(item -> {
            ShowSeat seat = item.getShowSeat();
            seat.setStatus(SeatStatus.BOOKED);
            seat.setLockedAt(null);
            seat.setLockedByUserId(null);
            showSeatRepository.save(seat);
        });

        // Update booking
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        booking.setPaymentId(paymentId);
        bookingRepository.save(booking);

        // Update available seat count on the show
        Show show = booking.getShow();
        int booked = booking.getTotalTickets();
        show.setAvailableSeats(Math.max(0, show.getAvailableSeats() - booked));
        showRepository.save(show);

        log.info("Booking {} CONFIRMED — paymentId={} seats={}",
            bookingReference, paymentId, booked);
    }

    private List<ShowSeat> lockSeats(List<Long> seatIds, Long showId, Long userId) {
        List<ShowSeat> showSeats = showSeatRepository.findByShowIdAndSeatIds(showId, seatIds);
        if (showSeats.size() != seatIds.size()) {
            throw new ResourceNotFoundException("One or more seats not found for this show");
        }
        List<String> unavailable = showSeats.stream()
            .filter(s -> s.getStatus() != SeatStatus.AVAILABLE)
            .map(s -> s.getSeat().getSeatNumber())
            .collect(Collectors.toList());
        if (!unavailable.isEmpty()) {
            throw new SeatUnavailableException("Seats already booked: " + unavailable);
        }
        showSeats.forEach(seat -> {
            seat.setStatus(SeatStatus.LOCKED);
            seat.setLockedAt(LocalDateTime.now());
            seat.setLockedByUserId(userId.toString());
        });
        return showSeatRepository.saveAll(showSeats);
    }

    private void validateShowBookable(Show show) {
        if (show.getStatus() != ShowStatus.SCHEDULED) {
            throw new BusinessException("Show is not available for booking");
        }
        if (show.getShowTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Cannot book tickets for past shows");
        }
    }

    private Booking createBooking(String ref, Long userId, Show show,
                                  List<ShowSeat> seats, PriceCalculationResponse pricing) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<BookingItem> items = seats.stream().map(showSeat ->
            BookingItem.builder()
                .showSeat(showSeat)
                .price(pricing.getSeats().stream()
                    .filter(s -> s.getSeatId().equals(showSeat.getSeat().getId()))
                    .findFirst().map(SeatDetail::getPrice).orElse(BigDecimal.ZERO))
                .build()
        ).collect(Collectors.toList());

        Booking booking = Booking.builder()
            .bookingReference(ref)
            .user(user)
            .show(show)
            .bookingItems(items)
            .totalTickets(seats.size())
            .baseAmount(pricing.getBaseAmount())
            .discountAmount(pricing.getDiscountAmount())
            .convenienceFee(pricing.getConvenienceFee())
            .taxAmount(pricing.getGst())
            .totalAmount(pricing.getTotalAmount())
            .status(BookingStatus.PENDING)
            .paymentStatus(PaymentStatus.PENDING)
            .build();

        items.forEach(item -> item.setBooking(booking));
        return bookingRepository.save(booking);
    }

    private String generateBookingReference() {
        return "CX-" + LocalDate.now().getYear() + "-"
            + String.format("%06d", (long)(Math.random() * 1_000_000));
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
            .bookingId(booking.getId())
            .bookingReference(booking.getBookingReference())
            .status(booking.getStatus().name())
            .paymentStatus(booking.getPaymentStatus().name())
            .movieTitle(booking.getShow().getMovie().getTitle())
            .theatreName(booking.getShow().getScreen().getTheatre().getName())
            .showTime(booking.getShow().getShowTime())
            .totalTickets(booking.getTotalTickets())
            .baseAmount(booking.getBaseAmount())
            .discountAmount(booking.getDiscountAmount())
            .convenienceFee(booking.getConvenienceFee())
            .taxAmount(booking.getTaxAmount())
            .totalAmount(booking.getTotalAmount())
            .build();
    }
}
