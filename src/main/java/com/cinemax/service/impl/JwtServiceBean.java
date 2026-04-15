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
// JwtServiceBean (Spring-managed JWT service)
// ─────────────────────────────────────────────
@Service
public class JwtServiceBean {

    private final String secret;
    private final long   expirationMs;
    private final long   refreshExpirationMs;

    JwtServiceBean(
            @org.springframework.beans.factory.annotation.Value("${jwt.secret}") String secret,
            @org.springframework.beans.factory.annotation.Value("${jwt.expiration:86400000}") long expirationMs,
            @org.springframework.beans.factory.annotation.Value("${jwt.refresh-expiration:604800000}") long refreshExpirationMs) {
        this.secret             = secret;
        this.expirationMs       = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public String generateToken(String email, String role) {
        return buildToken(email, role, expirationMs);
    }

    public String generateRefreshToken(String email) {
        return buildToken(email, "REFRESH", refreshExpirationMs);
    }

    private String buildToken(String email, String role, long ttlMs) {
        return io.jsonwebtoken.Jwts.builder()
            .setSubject(email)
            .claim("role", role)
            .setIssuedAt(new java.util.Date())
            .setExpiration(new java.util.Date(System.currentTimeMillis() + ttlMs))
            .signWith(getKey(), io.jsonwebtoken.SignatureAlgorithm.HS256)
            .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public boolean isTokenValid(String token,
            org.springframework.security.core.userdetails.UserDetails userDetails) {
        return extractEmail(token).equals(userDetails.getUsername()) && !isExpired(token);
    }

    private boolean isExpired(String token) {
        return getClaims(token).getExpiration().before(new java.util.Date());
    }

    private io.jsonwebtoken.Claims getClaims(String token) {
        return io.jsonwebtoken.Jwts.parserBuilder()
            .setSigningKey(getKey()).build()
            .parseClaimsJws(token).getBody();
    }

    private javax.crypto.SecretKey getKey() {
        byte[] keyBytes = java.util.Base64.getEncoder().encode(secret.getBytes());
        return io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
    }
}
