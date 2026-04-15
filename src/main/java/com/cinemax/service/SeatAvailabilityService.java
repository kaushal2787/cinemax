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
import java.util.List;
import java.util.stream.Collectors;

// ─────────────────────────────────────────────
// SeatAvailabilityService
// ─────────────────────────────────────────────
@Service
@RequiredArgsConstructor
public class SeatAvailabilityService {
    private final ShowSeatRepository showSeatRepository;

    public SeatLayoutResponse buildSeatLayout(Long showId) {
        List<ShowSeat> seats = showSeatRepository.findByShowId(showId);
        return SeatLayoutResponse.builder()
            .showId(showId)
            .seats(seats.stream().map(ss -> SeatAvailabilityDetail.builder()
                .seatId(ss.getSeat().getId())
                .seatNumber(ss.getSeat().getSeatNumber())
                .row(ss.getSeat().getRow())
                .category(ss.getSeat().getCategory().name())
                .status(ss.getStatus().name())
                .build()).collect(Collectors.toList()))
            .build();
    }
}
