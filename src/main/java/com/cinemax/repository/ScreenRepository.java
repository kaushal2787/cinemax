package com.cinemax.repository;

import com.cinemax.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// ─────────────────────────────────────────────
// Other repositories
// ─────────────────────────────────────────────
public interface ScreenRepository extends JpaRepository<Screen, Long> {}
