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

// ─────────────────────────────────────────────
// AuthServiceImpl
// ─────────────────────────────────────────────
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtServiceBean jwtServiceBean;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered");
        }

        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phone(request.getPhone())
            .city(request.getCity())
            .role(UserRole.CUSTOMER)
            .isVerified(false)
            .isActive(true)
            .build();

        User saved = userRepository.save(user);
        log.info("New customer registered: {}", saved.getEmail());

        String accessToken  = jwtServiceBean.generateToken(saved.getEmail(), saved.getRole().name());
        String refreshToken = jwtServiceBean.generateRefreshToken(saved.getEmail());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(86400L)
            .role(saved.getRole().name())
            .email(saved.getEmail())
            .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new BusinessException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String accessToken  = jwtServiceBean.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtServiceBean.generateRefreshToken(user.getEmail());

        log.info("User logged in: {}", user.getEmail());
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(86400L)
            .role(user.getRole().name())
            .email(user.getEmail())
            .build();
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        String email = jwtServiceBean.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtServiceBean.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(86400L)
            .role(user.getRole().name())
            .email(user.getEmail())
            .build();
    }
}
