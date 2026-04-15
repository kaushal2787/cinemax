package com.cinemax.service;

import com.cinemax.dto.*;
import com.cinemax.exception.BusinessException;
import com.cinemax.exception.ResourceNotFoundException;
import com.cinemax.exception.SeatUnavailableException;
import com.cinemax.exception.UnauthorizedException;
import com.cinemax.model.*;
import com.cinemax.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ─────────────────────────────────────────────
// PaymentService interface
// ─────────────────────────────────────────────
public interface PaymentService {
    String initiatePayment(Booking booking);
    void initiateRefund(Booking booking);
}
