package com.cinemax.repository;

import com.cinemax.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

// ─────────────────────────────────────────────
// OfferRepository
// ─────────────────────────────────────────────
public interface OfferRepository extends JpaRepository<Offer, Long> {

    @Query("""
        SELECT ss.seat.id         AS seatId,
               ss.seat.seatNumber AS seatNumber,
               ss.seat.row        AS row,
               ss.seat.category   AS category,
               CASE ss.seat.category
                 WHEN 'SILVER'   THEN s.silverPrice
                 WHEN 'GOLD'     THEN s.goldPrice
                 WHEN 'PLATINUM' THEN s.platinumPrice
                 WHEN 'RECLINER' THEN s.reclinePrice
                 ELSE s.silverPrice
               END AS price
        FROM ShowSeat ss
        JOIN ss.show s
        WHERE ss.show.id = :showId
          AND ss.seat.id IN :seatIds
        ORDER BY ss.seat.row, ss.seat.seatIndex
        """)
    List<SeatPriceInfo> findSeatPricesForShow(
        @Param("showId")  Long showId,
        @Param("seatIds") List<Long> seatIds
    );
}
