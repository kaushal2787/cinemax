package com.cinemax.repository;

import com.cinemax.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

// ─────────────────────────────────────────────
// ShowRepository
// ─────────────────────────────────────────────
public interface ShowRepository extends JpaRepository<Show, Long> {

    @Query("""
        SELECT s.id          AS showId,
               s.showTime    AS showTime,
               s.status      AS status,
               t.id          AS theatreId,
               t.name        AS theatreName,
               t.address     AS theatreAddress,
               t.city        AS city,
               t.rating      AS theatreRating,
               sc.name       AS screenName,
               sc.screenType AS screenType,
               m.id          AS movieId,
               m.title       AS movieTitle,
               m.language    AS language,
               m.certification AS certification,
               s.availableSeats AS availableSeats,
               s.totalSeats  AS totalSeats,
               s.silverPrice AS silverPrice,
               s.goldPrice   AS goldPrice,
               s.platinumPrice AS platinumPrice,
               s.reclinePrice AS reclinePrice
        FROM Show s
        JOIN s.screen sc
        JOIN sc.theatre t
        JOIN s.movie m
        WHERE m.id = :movieId
          AND t.city = :city
          AND s.showTime BETWEEN :from AND :to
          AND s.status = 'SCHEDULED'
          AND (:language IS NULL OR m.language = :language)
          AND (:screenType IS NULL OR sc.screenType = :screenType)
        ORDER BY t.rating DESC, s.showTime ASC
        """)
    List<ShowProjection> findShowsGroupedByTheatre(
        @Param("movieId")    Long movieId,
        @Param("city")       String city,
        @Param("from")       LocalDateTime from,
        @Param("to")         LocalDateTime to,
        @Param("language")   String language,
        @Param("screenType") String screenType
    );

    @Query("""
        SELECT COUNT(s) > 0 FROM Show s
        WHERE s.screen.id = :screenId
          AND s.status != 'CANCELLED'
          AND s.showTime < :end
          AND s.showEndTime > :start
        """)
    boolean existsOverlappingShow(
        @Param("screenId") Long screenId,
        @Param("start")    LocalDateTime start,
        @Param("end")      LocalDateTime end
    );
}
