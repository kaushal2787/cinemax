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
import com.cinemax.exception.*;
import java.util.List;
import java.util.stream.Collectors;

// ─────────────────────────────────────────────
// Movie Controller (B2C — public)
// ─────────────────────────────────────────────
@RestController
@RequestMapping("/api/v1/movies")
@Tag(name = "Movies", description = "Browse and search movies")
@RequiredArgsConstructor
public class MovieController {

    private final com.cinemax.repository.MovieRepository movieRepository;

    /** GET /api/v1/movies — list all now-showing movies */
    @GetMapping
    @Operation(summary = "List all now-showing movies")
    public ResponseEntity<ApiResponse<List<MovieSummaryResponse>>> listNowShowing(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String genre) {

        List<com.cinemax.model.Movie> movies = movieRepository.findNowShowing();

        List<MovieSummaryResponse> response = movies.stream()
            .filter(m -> language == null || language.equalsIgnoreCase(m.getLanguage()))
            .filter(m -> genre    == null || genre.equalsIgnoreCase(m.getGenre()))
            .map(m -> MovieSummaryResponse.builder()
                .id(m.getId())
                .title(m.getTitle())
                .language(m.getLanguage())
                .genre(m.getGenre())
                .durationMinutes(m.getDurationMinutes())
                .certification(m.getCertification())
                .posterUrl(m.getPosterUrl())
                .releaseDate(m.getReleaseDate())
                .status(m.getStatus().name())
                .build())
            .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** GET /api/v1/movies/{id} — get movie details */
    @GetMapping("/{id}")
    @Operation(summary = "Get movie details")
    public ResponseEntity<ApiResponse<MovieSummaryResponse>> getMovie(
            @PathVariable Long id) {

        com.cinemax.model.Movie m = movieRepository.findById(id)
            .orElseThrow(() -> new com.cinemax.exception.ResourceNotFoundException("Movie not found"));

        return ResponseEntity.ok(ApiResponse.success(MovieSummaryResponse.builder()
            .id(m.getId())
            .title(m.getTitle())
            .language(m.getLanguage())
            .genre(m.getGenre())
            .durationMinutes(m.getDurationMinutes())
            .certification(m.getCertification())
            .posterUrl(m.getPosterUrl())
            .trailerUrl(m.getTrailerUrl())
            .description(m.getDescription())
            .director(m.getDirector())
            .cast(m.getMovieCast())
            .releaseDate(m.getReleaseDate())
            .status(m.getStatus().name())
            .build()));
    }
}
