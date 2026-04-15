package com.cinemax.service;

import com.cinemax.dto.PriceCalculationRequest;
import com.cinemax.dto.PriceCalculationResponse;
import com.cinemax.repository.OfferRepository;
import com.cinemax.repository.SeatPriceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OfferService — discount engine")
class OfferServiceTest {

    @Mock
    private OfferRepository offerRepository;

    @InjectMocks
    private OfferService offerService;

    // Fixture: 3 seats at ₹200 each
    private static final BigDecimal SEAT_PRICE    = new BigDecimal("200.00");
    private static final Long       SHOW_ID       = 1L;
    private static final List<Long> THREE_SEAT_IDS = List.of(101L, 102L, 103L);

    // ── helpers ──────────────────────────────

    private SeatPriceInfo seatInfo(Long id, String num, BigDecimal price) {
        return new SeatPriceInfo() {
            public Long getSeatId()      { return id; }
            public String getSeatNumber(){ return num; }
            public String getRow()       { return "A"; }
            public String getCategory()  { return "GOLD"; }
            public BigDecimal getPrice() { return price; }
        };
    }

    private PriceCalculationRequest request(Long showId, List<Long> seatIds) {
        return PriceCalculationRequest.builder()
            .showId(showId).seatIds(seatIds).build();
    }

    @BeforeEach
    void setupMocks() {
        when(offerRepository.findSeatPricesForShow(anyLong(), anyList()))
            .thenReturn(List.of(
                seatInfo(101L, "A1", SEAT_PRICE),
                seatInfo(102L, "A2", SEAT_PRICE),
                seatInfo(103L, "A3", SEAT_PRICE)
            ));
    }

    // ─────────────────────────────────────────
    @Nested
    @DisplayName("Third ticket 50% discount")
    class ThirdTicketDiscount {

        @Test
        @DisplayName("3 tickets: only 3rd ticket gets 50% off")
        void thirdTicketGets50PercentDiscount() {
            LocalDateTime eveningShow = LocalDateTime.now().withHour(20).withMinute(0);

            PriceCalculationResponse result =
                offerService.calculatePrice(request(SHOW_ID, THREE_SEAT_IDS), eveningShow);

            // Base: 3 × 200 = 600
            assertThat(result.getBaseAmount()).isEqualByComparingTo("600.00");
            // Discount: 50% of ticket 3 = 100
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("100.00");
        }

        @Test
        @DisplayName("2 tickets: no third-ticket discount applied")
        void twoTicketsNoDiscount() {
            when(offerRepository.findSeatPricesForShow(anyLong(), anyList()))
                .thenReturn(List.of(
                    seatInfo(101L, "A1", SEAT_PRICE),
                    seatInfo(102L, "A2", SEAT_PRICE)
                ));
            LocalDateTime eveningShow = LocalDateTime.now().withHour(20).withMinute(0);

            PriceCalculationResponse result =
                offerService.calculatePrice(request(SHOW_ID, List.of(101L, 102L)), eveningShow);

            assertThat(result.getBaseAmount()).isEqualByComparingTo("400.00");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("6 tickets: tickets 3 and 6 each get 50% off")
        void sixTicketsTwoDiscounts() {
            when(offerRepository.findSeatPricesForShow(anyLong(), anyList()))
                .thenReturn(List.of(
                    seatInfo(101L, "A1", SEAT_PRICE),
                    seatInfo(102L, "A2", SEAT_PRICE),
                    seatInfo(103L, "A3", SEAT_PRICE),
                    seatInfo(104L, "A4", SEAT_PRICE),
                    seatInfo(105L, "A5", SEAT_PRICE),
                    seatInfo(106L, "A6", SEAT_PRICE)
                ));
            LocalDateTime eveningShow = LocalDateTime.now().withHour(20).withMinute(0);

            PriceCalculationResponse result =
                offerService.calculatePrice(request(SHOW_ID, List.of(101L,102L,103L,104L,105L,106L)), eveningShow);

            assertThat(result.getBaseAmount()).isEqualByComparingTo("1200.00");
            // Tickets 3 and 6: 2 × 100 = 200
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("200.00");
        }
    }

    // ─────────────────────────────────────────
    @Nested
    @DisplayName("Afternoon show 20% discount")
    class AfternoonShowDiscount {

        @Test
        @DisplayName("12:00 show qualifies for 20% off all tickets")
        void noonShowQualifies() {
            LocalDateTime noonShow = LocalDateTime.now().withHour(12).withMinute(0);

            PriceCalculationResponse result =
                offerService.calculatePrice(request(SHOW_ID, THREE_SEAT_IDS), noonShow);

            // Tickets 1 and 2: 20% off (₹40 each) = ₹80
            // Ticket 3: 50% off (better than 20%) = ₹100
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("180.00");
        }

        @Test
        @DisplayName("16:59 show still qualifies (boundary)")
        void endBoundaryQualifies() {
            LocalDateTime lateAfternoonShow = LocalDateTime.now().withHour(16).withMinute(59);
            assertThat(offerService.isAfternoonShow(lateAfternoonShow.toLocalTime())).isTrue();
        }

        @Test
        @DisplayName("17:00 show does NOT qualify (just outside boundary)")
        void eveningShowDoesNotQualify() {
            LocalDateTime eveningShow = LocalDateTime.now().withHour(17).withMinute(0);
            assertThat(offerService.isAfternoonShow(eveningShow.toLocalTime())).isFalse();
        }

        @Test
        @DisplayName("11:59 morning show does NOT qualify")
        void morningShowDoesNotQualify() {
            LocalDateTime morningShow = LocalDateTime.now().withHour(11).withMinute(59);
            assertThat(offerService.isAfternoonShow(morningShow.toLocalTime())).isFalse();
        }
    }

    // ─────────────────────────────────────────
    @Nested
    @DisplayName("Combined discounts — best wins per ticket")
    class CombinedDiscounts {

        @Test
        @DisplayName("Afternoon + 3rd ticket: 3rd ticket gets 50% (better than 20%)")
        void thirdTicketWinsOverAfternoon() {
            LocalDateTime afternoonShow = LocalDateTime.now().withHour(14).withMinute(30);

            PriceCalculationResponse result =
                offerService.calculatePrice(request(SHOW_ID, THREE_SEAT_IDS), afternoonShow);

            // Ticket 1: 20% off = ₹40
            // Ticket 2: 20% off = ₹40
            // Ticket 3: 50% off (wins) = ₹100
            BigDecimal expectedDiscount = new BigDecimal("180.00");
            assertThat(result.getDiscountAmount()).isEqualByComparingTo(expectedDiscount);
        }
    }

    // ─────────────────────────────────────────
    @Nested
    @DisplayName("Price breakdown — fees and taxes")
    class PriceBreakdown {

        @Test
        @DisplayName("Convenience fee is 2% of after-discount amount")
        void convenienceFeeCalculation() {
            LocalDateTime eveningShow = LocalDateTime.now().withHour(20);
            // 3 tickets × ₹200 = ₹600, 3rd gets 50% = ₹100 discount → ₹500 after discount
            // Convenience fee = 2% of ₹500 = ₹10

            PriceCalculationResponse result =
                offerService.calculatePrice(request(SHOW_ID, THREE_SEAT_IDS), eveningShow);

            assertThat(result.getConvenienceFee()).isEqualByComparingTo("10.00");
        }

        @Test
        @DisplayName("GST is 18% of (after-discount + convenience fee)")
        void gstCalculation() {
            LocalDateTime eveningShow = LocalDateTime.now().withHour(20);
            // After discount ₹500 + convenience ₹10 = ₹510 → GST = 18% = ₹91.80

            PriceCalculationResponse result =
                offerService.calculatePrice(request(SHOW_ID, THREE_SEAT_IDS), eveningShow);

            assertThat(result.getGst()).isEqualByComparingTo("91.80");
        }

        @Test
        @DisplayName("Total = after-discount + convenience fee + GST")
        void totalAmountIsCorrect() {
            LocalDateTime eveningShow = LocalDateTime.now().withHour(20);
            // ₹500 + ₹10 + ₹91.80 = ₹601.80

            PriceCalculationResponse result =
                offerService.calculatePrice(request(SHOW_ID, THREE_SEAT_IDS), eveningShow);

            assertThat(result.getTotalAmount()).isEqualByComparingTo("601.80");
        }
    }
}
