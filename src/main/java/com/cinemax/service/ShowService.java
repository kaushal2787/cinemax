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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// ─────────────────────────────────────────────
// ShowService — READ SCENARIO
// ─────────────────────────────────────────────
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ShowService {

    private final ShowRepository showRepository;
    private final OfferService offerService;
    private final SeatAvailabilityService seatAvailabilityService;

    private static final LocalTime AFTERNOON_START = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_END   = LocalTime.of(16, 59);

    /**
     * READ: Browse theatres showing a movie in a city on a date.
     * Cached in Redis for 15 minutes.
     */
    @Cacheable(value = "shows", key = "#request.city + '_' + #request.movieId + '_' + #request.date")
    public List<TheatreShowsResponse> browseShowsByMovieAndCity(ShowSearchRequest request) {
        log.info("Fetching shows: movie={} city={} date={}", request.getMovieId(), request.getCity(), request.getDate());
        LocalDateTime dayStart = request.getDate().atStartOfDay();
        LocalDateTime dayEnd   = request.getDate().atTime(LocalTime.MAX);

        List<ShowProjection> projections = showRepository.findShowsGroupedByTheatre(
            request.getMovieId(), request.getCity(), dayStart, dayEnd,
            request.getLanguage(), request.getScreenType()
        );

        return projections.stream()
            .collect(Collectors.groupingBy(ShowProjection::getTheatreId))
            .entrySet().stream()
            .map(entry -> buildTheatreShowsResponse(entry.getKey(), entry.getValue()))
            .sorted((a, b) -> Double.compare(b.getRating() == null ? 0 : b.getRating(),
                                              a.getRating() == null ? 0 : a.getRating()))
            .collect(Collectors.toList());
    }

    private TheatreShowsResponse buildTheatreShowsResponse(Long theatreId, List<ShowProjection> projections) {
        ShowProjection first = projections.get(0);
        List<ShowResponse> shows = projections.stream()
            .map(this::buildShowResponse)
            .sorted(Comparator.comparing(ShowResponse::getShowTime))
            .collect(Collectors.toList());

        return TheatreShowsResponse.builder()
            .theatreId(theatreId)
            .theatreName(first.getTheatreName())
            .address(first.getTheatreAddress())
            .city(first.getCity())
            .rating(first.getTheatreRating())
            .shows(shows)
            .build();
    }

    private ShowResponse buildShowResponse(ShowProjection p) {
        String period = determineShowPeriod(p.getShowTime().toLocalTime());
        boolean isFastFilling = p.getAvailableSeats() != null && p.getTotalSeats() != null
            && p.getAvailableSeats() <= (p.getTotalSeats() * 0.2);

        return ShowResponse.builder()
            .showId(p.getShowId())
            .showTime(p.getShowTime())
            .showTimeDisplay(p.getShowTime().toLocalTime().toString())
            .showPeriod(period)
            .status(p.getStatus())
            .theatreId(p.getTheatreId())
            .theatreName(p.getTheatreName())
            .theatreAddress(p.getTheatreAddress())
            .city(p.getCity())
            .screenName(p.getScreenName())
            .screenType(p.getScreenType())
            .movieId(p.getMovieId())
            .movieTitle(p.getMovieTitle())
            .language(p.getLanguage())
            .certification(p.getCertification())
            .availableSeats(p.getAvailableSeats())
            .totalSeats(p.getTotalSeats())
            .isFastFilling(isFastFilling)
            .isSoldOut(p.getAvailableSeats() != null && p.getAvailableSeats() == 0)
            .silverPrice(p.getSilverPrice())
            .goldPrice(p.getGoldPrice())
            .platinumPrice(p.getPlatinumPrice())
            .reclinePrice(p.getReclinePrice())
            .applicableOffers(offerService.getApplicableOffers(p.getShowId(), p.getCity(), period))
            .build();
    }

    public String determineShowPeriod(LocalTime time) {
        if (time.isBefore(AFTERNOON_START))  return "MORNING";
        if (!time.isAfter(AFTERNOON_END))     return "AFTERNOON";
        if (time.isBefore(LocalTime.of(20, 0))) return "EVENING";
        return "NIGHT";
    }

    @Cacheable(value = "seatLayout", key = "#showId")
    public SeatLayoutResponse getSeatLayout(Long showId) {
        return seatAvailabilityService.buildSeatLayout(showId);
    }

    public PriceCalculationResponse calculatePrice(PriceCalculationRequest request) {
        Show show = showRepository.findById(request.getShowId())
            .orElseThrow(() -> new ResourceNotFoundException("Show not found"));
        return offerService.calculatePrice(request, show.getShowTime());
    }
}
