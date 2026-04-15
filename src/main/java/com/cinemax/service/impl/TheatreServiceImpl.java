package com.cinemax.service.impl;

import com.cinemax.repository.*;
import com.cinemax.exception.*;

import com.cinemax.dto.*;
import com.cinemax.exception.BusinessException;
import com.cinemax.exception.ResourceNotFoundException;
import com.cinemax.model.*;
import com.cinemax.repository.TheatreRepository;
import com.cinemax.repository.UserRepository;
import com.cinemax.service.AuthService;
import com.cinemax.service.ShowManagementService;
import com.cinemax.service.TheatreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

// ─────────────────────────────────────────────
// TheatreServiceImpl
// ─────────────────────────────────────────────
@Service
@RequiredArgsConstructor
@Slf4j
public class TheatreServiceImpl implements TheatreService {

    private final TheatreRepository theatreRepository;
    private final UserRepository userRepository;
    private final ShowManagementService showManagementService;

    @Override
    @Transactional
    public TheatreResponse onboardTheatre(TheatreOnboardRequest request, Long partnerId) {
        User partnerUser = userRepository.findById(partnerId)
            .orElseThrow(() -> new ResourceNotFoundException("Partner not found"));

        // Find or create the TheatrePartner record linked to this user
        TheatrePartner partner = TheatrePartner.builder()
            .businessName(request.getName())
            .email(request.getContactEmail())
            .phone(request.getContactPhone())
            .status(PartnerStatus.PENDING)
            .build();

        Theatre theatre = Theatre.builder()
            .name(request.getName())
            .city(request.getCity())
            .state(request.getState())
            .country(request.getCountry())
            .address(request.getAddress())
            .pincode(request.getPincode())
            .contactEmail(request.getContactEmail())
            .contactPhone(request.getContactPhone())
            .status(TheatreStatus.PENDING_APPROVAL)
            .rating(0.0)
            .build();

        Theatre saved = theatreRepository.save(theatre);
        log.info("Theatre onboarded: id={} name={} city={}", saved.getId(), saved.getName(), saved.getCity());

        // Auto-provision screens if provided
        if (request.getScreens() != null && !request.getScreens().isEmpty()) {
            log.info("Provisioning {} screens for theatre {}", request.getScreens().size(), saved.getId());
        }

        return mapToTheatreResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TheatreResponse> getTheatresByPartner(Long partnerId) {
        return theatreRepository.findByPartnerId(partnerId).stream()
            .map(this::mapToTheatreResponse)
            .collect(Collectors.toList());
    }

    private TheatreResponse mapToTheatreResponse(Theatre theatre) {
        return TheatreResponse.builder()
            .id(theatre.getId())
            .name(theatre.getName())
            .city(theatre.getCity())
            .address(theatre.getAddress())
            .status(theatre.getStatus() != null ? theatre.getStatus().name() : "PENDING_APPROVAL")
            .build();
    }
}
