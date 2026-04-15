package com.cinemax.repository;

import com.cinemax.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TheatreRepository extends JpaRepository<Theatre, Long> {
    List<Theatre> findByPartnerId(Long partnerId);
    List<Theatre> findByStatus(TheatreStatus status);

    @Query("SELECT t FROM Theatre t WHERE t.city = :city AND t.status = 'ACTIVE'")
    List<Theatre> findActiveByCity(@Param("city") String city);
}
