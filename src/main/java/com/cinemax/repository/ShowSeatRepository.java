package com.cinemax.repository;

import com.cinemax.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

// ─────────────────────────────────────────────
// ShowSeatRepository
// ─────────────────────────────────────────────
public interface ShowSeatRepository extends JpaRepository<ShowSeat, Long> {

    @Query("""
        SELECT ss FROM ShowSeat ss
        JOIN FETCH ss.seat seat
        WHERE ss.show.id = :showId
          AND seat.id IN :seatIds
        """)
    List<ShowSeat> findByShowIdAndSeatIds(
        @Param("showId")  Long showId,
        @Param("seatIds") List<Long> seatIds
    );

    @Query("SELECT ss FROM ShowSeat ss JOIN FETCH ss.seat WHERE ss.show.id = :showId")
    List<ShowSeat> findByShowId(@Param("showId") Long showId);
}
