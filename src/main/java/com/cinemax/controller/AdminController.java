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
// Admin Controller
// ─────────────────────────────────────────────
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin", description = "Platform administration")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final com.cinemax.repository.TheatreRepository theatreRepository;
    private final com.cinemax.repository.MovieRepository    movieRepository;
    private final com.cinemax.repository.UserRepository     userRepository;

    /** GET /api/v1/admin/theatres/pending — list theatres awaiting approval */
    @GetMapping("/theatres/pending")
    @Operation(summary = "List theatres pending approval")
    public ResponseEntity<ApiResponse<List<TheatreResponse>>> getPendingTheatres() {
        List<com.cinemax.model.Theatre> pending =
            theatreRepository.findByStatus(com.cinemax.model.TheatreStatus.PENDING_APPROVAL);

        List<TheatreResponse> response = pending.stream()
            .map(t -> TheatreResponse.builder()
                .id(t.getId()).name(t.getName())
                .city(t.getCity()).address(t.getAddress())
                .status(t.getStatus().name()).build())
            .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(response,
            pending.size() + " theatres pending approval"));
    }

    /** POST /api/v1/admin/theatres/{id}/approve — approve a theatre */
    @PostMapping("/theatres/{id}/approve")
    @Operation(summary = "Approve a theatre onboarding request")
    public ResponseEntity<ApiResponse<Void>> approveTheatre(@PathVariable Long id) {
        com.cinemax.model.Theatre theatre = theatreRepository.findById(id)
            .orElseThrow(() -> new com.cinemax.exception.ResourceNotFoundException("Theatre not found"));
        theatre.setStatus(com.cinemax.model.TheatreStatus.ACTIVE);
        theatreRepository.save(theatre);
        return ResponseEntity.ok(ApiResponse.success(null, "Theatre approved successfully"));
    }

    /** POST /api/v1/admin/theatres/{id}/suspend — suspend a theatre */
    @PostMapping("/theatres/{id}/suspend")
    @Operation(summary = "Suspend a theatre")
    public ResponseEntity<ApiResponse<Void>> suspendTheatre(@PathVariable Long id) {
        com.cinemax.model.Theatre theatre = theatreRepository.findById(id)
            .orElseThrow(() -> new com.cinemax.exception.ResourceNotFoundException("Theatre not found"));
        theatre.setStatus(com.cinemax.model.TheatreStatus.SUSPENDED);
        theatreRepository.save(theatre);
        return ResponseEntity.ok(ApiResponse.success(null, "Theatre suspended"));
    }

    /** POST /api/v1/admin/movies — add a new movie to the platform */
    @PostMapping("/movies")
    @Operation(summary = "Add a new movie")
    public ResponseEntity<ApiResponse<MovieSummaryResponse>> addMovie(
            @Valid @RequestBody MovieCreateRequest request) {

        com.cinemax.model.Movie movie = com.cinemax.model.Movie.builder()
            .title(request.getTitle())
            .description(request.getDescription())
            .director(request.getDirector())
            .cast(request.getCast())
            .durationMinutes(request.getDurationMinutes())
            .language(request.getLanguage())
            .genre(request.getGenre())
            .certification(request.getCertification())
            .posterUrl(request.getPosterUrl())
            .trailerUrl(request.getTrailerUrl())
            .releaseDate(request.getReleaseDate())
            .status(com.cinemax.model.MovieStatus.UPCOMING)
            .build();

        com.cinemax.model.Movie saved = movieRepository.save(movie);
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(MovieSummaryResponse.builder()
                .id(saved.getId()).title(saved.getTitle())
                .language(saved.getLanguage()).status(saved.getStatus().name()).build(),
            "Movie added successfully"));
    }

    /** GET /api/v1/admin/stats — platform stats */
    @GetMapping("/stats")
    @Operation(summary = "Get platform stats")
    public ResponseEntity<ApiResponse<PlatformStatsResponse>> getStats() {
        long totalUsers    = userRepository.count();
        long totalTheatres = theatreRepository.count();
        long totalMovies   = movieRepository.count();

        return ResponseEntity.ok(ApiResponse.success(
            PlatformStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalTheatres(totalTheatres)
                .totalMovies(totalMovies)
                .build()));
    }
}
