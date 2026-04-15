package com.cinemax.repository;

import com.cinemax.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

// ─────────────────────────────────────────────
// SeatPriceInfo projection
// ─────────────────────────────────────────────
public interface SeatPriceInfo {
    Long       getSeatId();
    String     getSeatNumber();
    String     getRow();
    String     getCategory();
    BigDecimal getPrice();
}
