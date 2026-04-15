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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// ─────────────────────────────────────────────
// OfferService — DISCOUNT ENGINE
// ─────────────────────────────────────────────
@Service
@RequiredArgsConstructor
@Slf4j
public class OfferService {

    private static final BigDecimal THIRD_TICKET_DISCOUNT   = new BigDecimal("0.50");
    private static final BigDecimal AFTERNOON_SHOW_DISCOUNT = new BigDecimal("0.20");
    private static final BigDecimal CONVENIENCE_FEE_RATE    = new BigDecimal("0.02");
    private static final BigDecimal GST_RATE                = new BigDecimal("0.18");
    private static final LocalTime  AFTERNOON_START         = LocalTime.of(12, 0);
    private static final LocalTime  AFTERNOON_END           = LocalTime.of(16, 59);

    private final OfferRepository offerRepository;

    /**
     * Apply discount rules:
     * 1. Every 3rd ticket in a booking = 50% off
     * 2. Afternoon show (12:00–16:59) = 20% off ALL tickets
     * Best discount per ticket wins; discounts do NOT stack.
     */
    public PriceCalculationResponse calculatePrice(PriceCalculationRequest request, LocalDateTime showTime) {
        List<SeatPriceInfo> seatPrices = offerRepository.findSeatPricesForShow(
            request.getShowId(), request.getSeatIds());
        boolean isAfternoonShow = isAfternoonShow(showTime.toLocalTime());

        BigDecimal baseAmount    = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        List<SeatDetail> seatDetails = new ArrayList<>();
        StringBuilder discountBreakdown = new StringBuilder();

        if (isAfternoonShow) {
            discountBreakdown.append("Afternoon show 20% off applied. ");
        }

        for (int i = 0; i < seatPrices.size(); i++) {
            SeatPriceInfo info     = seatPrices.get(i);
            BigDecimal seatPrice   = info.getPrice();
            baseAmount             = baseAmount.add(seatPrice);
            int ticketNumber       = i + 1;
            boolean isThirdTicket  = (ticketNumber % 3 == 0);

            BigDecimal discount     = BigDecimal.ZERO;
            String discountReason   = "NONE";

            if (isThirdTicket) {
                // 50% is always better than 20%; apply 50% regardless
                discount      = seatPrice.multiply(THIRD_TICKET_DISCOUNT);
                discountReason = "THIRD_TICKET_50%";
                discountBreakdown.append(String.format("Ticket #%d: 50%% off (₹%.2f). ", ticketNumber, discount));
            } else if (isAfternoonShow) {
                discount      = seatPrice.multiply(AFTERNOON_SHOW_DISCOUNT);
                discountReason = "AFTERNOON_SHOW_20%";
            }

            totalDiscount = totalDiscount.add(discount);

            seatDetails.add(SeatDetail.builder()
                .seatId(info.getSeatId())
                .seatNumber(info.getSeatNumber())
                .row(info.getRow())
                .category(info.getCategory())
                .price(seatPrice)
                .discount(discount)
                .build());
        }

        BigDecimal afterDiscount  = baseAmount.subtract(totalDiscount);
        BigDecimal convenienceFee = afterDiscount.multiply(CONVENIENCE_FEE_RATE);
        BigDecimal gst            = afterDiscount.add(convenienceFee).multiply(GST_RATE);
        BigDecimal totalAmount    = afterDiscount.add(convenienceFee).add(gst);

        return PriceCalculationResponse.builder()
            .seats(seatDetails)
            .baseAmount(baseAmount)
            .discountAmount(totalDiscount)
            .discountBreakdown(discountBreakdown.toString().trim())
            .convenienceFee(convenienceFee)
            .gst(gst)
            .totalAmount(totalAmount)
            .build();
    }

    public boolean isAfternoonShow(LocalTime time) {
        return !time.isBefore(AFTERNOON_START) && !time.isAfter(AFTERNOON_END);
    }

    public List<OfferSummary> getApplicableOffers(Long showId, String city, String period) {
        List<OfferSummary> offers = new ArrayList<>();
        offers.add(OfferSummary.builder()
            .offerCode("THIRD50")
            .description("50% off on every 3rd ticket")
            .offerType("THIRD_TICKET_50_PERCENT")
            .discountPercentage(new BigDecimal("50"))
            .build());
        if ("AFTERNOON".equals(period)) {
            offers.add(OfferSummary.builder()
                .offerCode("AFTERNOON20")
                .description("20% off on afternoon shows (12PM–5PM)")
                .offerType("AFTERNOON_20_PERCENT")
                .discountPercentage(new BigDecimal("20"))
                .build());
        }
        return offers;
    }
}
