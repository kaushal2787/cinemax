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
import java.util.List;
import java.util.stream.Collectors;

// ─────────────────────────────────────────────
// ShowManagementService — B2B WRITE SCENARIO
// ─────────────────────────────────────────────
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowManagementService {

    private final ShowRepository     showRepository;
    private final ScreenRepository   screenRepository;
    private final MovieRepository    movieRepository;
    private final ShowSeatRepository showSeatRepository;

    @Transactional
    public ShowResponse createShow(CreateShowRequest request, Long partnerId) {
        Screen screen = screenRepository.findById(request.getScreenId())
            .orElseThrow(() -> new ResourceNotFoundException("Screen not found"));
        validatePartnerOwnsScreen(screen, partnerId);

        Movie movie = movieRepository.findById(request.getMovieId())
            .orElseThrow(() -> new ResourceNotFoundException("Movie not found"));

        LocalDateTime endTime = request.getShowTime().plusMinutes(movie.getDurationMinutes());
        if (showRepository.existsOverlappingShow(screen.getId(), request.getShowTime(), endTime)) {
            throw new BusinessException("Another show is already scheduled at this time in this screen");
        }

        Show show = Show.builder()
            .movie(movie)
            .screen(screen)
            .showTime(request.getShowTime())
            .showEndTime(endTime)
            .silverPrice(request.getSilverPrice())
            .goldPrice(request.getGoldPrice())
            .platinumPrice(request.getPlatinumPrice())
            .reclinePrice(request.getReclinePrice())
            .status(ShowStatus.SCHEDULED)
            .totalSeats(screen.getTotalSeats())
            .availableSeats(screen.getTotalSeats())
            .build();

        Show saved = showRepository.save(show);
        allocateSeatInventory(saved, screen);
        log.info("Show created: id={} movie={}", saved.getId(), movie.getTitle());
        return mapToShowResponse(saved);
    }

    @Transactional
    public ShowResponse updateShow(Long showId, UpdateShowRequest request, Long partnerId) {
        Show show = showRepository.findById(showId)
            .orElseThrow(() -> new ResourceNotFoundException("Show not found"));
        validatePartnerOwnsShow(show, partnerId);

        if (show.getStatus() == ShowStatus.COMPLETED || show.getStatus() == ShowStatus.RUNNING) {
            throw new BusinessException("Cannot update a completed or running show");
        }
        if (request.getShowTime()      != null) show.setShowTime(request.getShowTime());
        if (request.getSilverPrice()   != null) show.setSilverPrice(request.getSilverPrice());
        if (request.getGoldPrice()     != null) show.setGoldPrice(request.getGoldPrice());
        if (request.getPlatinumPrice() != null) show.setPlatinumPrice(request.getPlatinumPrice());
        if (request.getReclinePrice()  != null) show.setReclinePrice(request.getReclinePrice());
        if (request.getStatus()        != null) show.setStatus(ShowStatus.valueOf(request.getStatus()));

        return mapToShowResponse(showRepository.save(show));
    }

    @Transactional
    public void deleteShow(Long showId, Long partnerId) {
        Show show = showRepository.findById(showId)
            .orElseThrow(() -> new ResourceNotFoundException("Show not found"));
        validatePartnerOwnsShow(show, partnerId);
        if (show.getStatus() == ShowStatus.RUNNING) {
            throw new BusinessException("Cannot delete a running show");
        }
        show.setStatus(ShowStatus.CANCELLED);
        showRepository.save(show);
        log.warn("Show {} cancelled — triggering refund process", showId);
    }

    @Transactional
    public void reallocateSeatInventory(Long showId, Long partnerId) {
        Show show = showRepository.findById(showId)
            .orElseThrow(() -> new ResourceNotFoundException("Show not found"));
        validatePartnerOwnsShow(show, partnerId);
        allocateSeatInventory(show, show.getScreen());
    }

    public void allocateSeatInventory(Show show, Screen screen) {
        List<ShowSeat> showSeats = screen.getSeats().stream()
            .map(seat -> ShowSeat.builder()
                .show(show).seat(seat)
                .status(SeatStatus.AVAILABLE)
                .version(0L)
                .build())
            .collect(Collectors.toList());
        showSeatRepository.saveAll(showSeats);
        log.info("Allocated {} seats for show {}", showSeats.size(), show.getId());
    }

    private void validatePartnerOwnsScreen(Screen screen, Long partnerId) {
        if (!screen.getTheatre().getPartner().getId().equals(partnerId)) {
            throw new UnauthorizedException("You don't own this screen");
        }
    }

    private void validatePartnerOwnsShow(Show show, Long partnerId) {
        if (!show.getScreen().getTheatre().getPartner().getId().equals(partnerId)) {
            throw new UnauthorizedException("You don't own this show");
        }
    }

    private ShowResponse mapToShowResponse(Show show) {
        return ShowResponse.builder()
            .showId(show.getId())
            .showTime(show.getShowTime())
            .status(show.getStatus())
            .theatreName(show.getScreen().getTheatre().getName())
            .screenName(show.getScreen().getName())
            .movieTitle(show.getMovie().getTitle())
            .availableSeats(show.getAvailableSeats())
            .totalSeats(show.getTotalSeats())
            .silverPrice(show.getSilverPrice())
            .goldPrice(show.getGoldPrice())
            .platinumPrice(show.getPlatinumPrice())
            .build();
    }
}
