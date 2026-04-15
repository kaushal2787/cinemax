package com.cinemax.repository;

import com.cinemax.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

// ─────────────────────────────────────────────
// BookingRepository
// ─────────────────────────────────────────────
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
        SELECT b FROM Booking b
        WHERE b.bookingReference = :ref
          AND b.user.id = :userId
        """)
    Optional<Booking> findByBookingReferenceAndUserId(
        @Param("ref")    String ref,
        @Param("userId") Long userId
    );

    Optional<Booking> findByBookingReference(String bookingReference);
}
