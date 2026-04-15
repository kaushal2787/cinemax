package com.cinemax.repository;

import com.cinemax.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;

// ─────────────────────────────────────────────
// ShowProjection
// ─────────────────────────────────────────────
public interface ShowProjection {
    Long          getShowId();
    LocalDateTime getShowTime();
    ShowStatus    getStatus();
    Long          getTheatreId();
    String        getTheatreName();
    String        getTheatreAddress();
    String        getCity();
    Double        getTheatreRating();
    String        getScreenName();
    String        getScreenType();
    Long          getMovieId();
    String        getMovieTitle();
    String        getLanguage();
    String        getCertification();
    Integer       getAvailableSeats();
    Integer       getTotalSeats();
    BigDecimal    getSilverPrice();
    BigDecimal    getGoldPrice();
    BigDecimal    getPlatinumPrice();
    BigDecimal    getReclinePrice();
}
