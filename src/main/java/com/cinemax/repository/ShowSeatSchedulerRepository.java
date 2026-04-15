package com.cinemax.repository;

import com.cinemax.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ShowSeatSchedulerRepository extends JpaRepository<ShowSeat, Long> {
    @Query("""
        SELECT ss FROM ShowSeat ss
        WHERE ss.status = 'LOCKED'
          AND ss.lockedAt < :expiryTime
        """)
    List<ShowSeat> findExpiredLocks(@Param("expiryTime") LocalDateTime expiryTime);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("""
        UPDATE ShowSeat ss SET ss.status = 'AVAILABLE',
        ss.lockedAt = NULL, ss.lockedByUserId = NULL
        WHERE ss.status = 'LOCKED'
          AND ss.lockedAt < :expiryTime
        """)
    int releaseExpiredLocks(@Param("expiryTime") LocalDateTime expiryTime);
}
