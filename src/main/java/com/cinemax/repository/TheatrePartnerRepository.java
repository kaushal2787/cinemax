package com.cinemax.repository;

import com.cinemax.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface TheatrePartnerRepository extends JpaRepository<TheatrePartner, Long> {
    Optional<TheatrePartner> findByEmail(String email);
}
