package com.cinemax.controller;

import com.cinemax.dto.ApiResponse;
import com.cinemax.exception.BusinessException;
import com.cinemax.exception.ResourceNotFoundException;
import com.cinemax.exception.SeatUnavailableException;
import com.cinemax.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.cinemax.model.*;

// ─────────────────────────────────────────────
// Payment Webhook Controller
// ─────────────────────────────────────────────
@org.springframework.web.bind.annotation.RestController
@org.springframework.web.bind.annotation.RequestMapping("/api/v1/webhooks")
@Slf4j
public class PaymentWebhookController {

    private final com.cinemax.service.BookingService bookingService;
    private final com.cinemax.service.impl.RazorpayPaymentServiceImpl paymentService;

    PaymentWebhookController(
            com.cinemax.service.BookingService bookingService,
            com.cinemax.service.impl.RazorpayPaymentServiceImpl paymentService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
    }

    /**
     * Razorpay webhook endpoint.
     * Events: payment.captured → confirm, payment.failed → release, refund.processed → ack.
     * Security: HMAC-SHA256 verified against X-Razorpay-Signature header.
     */
    @org.springframework.web.bind.annotation.PostMapping("/razorpay")
    public ResponseEntity<Void> handleRazorpayWebhook(
            @org.springframework.web.bind.annotation.RequestHeader(
                value = "X-Razorpay-Signature", required = false) String signature,
            @org.springframework.web.bind.annotation.RequestBody String payload) {

        if (signature == null || !paymentService.verifyWebhookSignature(payload, signature)) {
            log.warn("Razorpay webhook rejected — invalid or missing signature");
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED).build();
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(payload);

            String event     = root.path("event").asText();
            com.fasterxml.jackson.databind.JsonNode entity =
                root.path("payload").path("payment").path("entity");

            String paymentId  = entity.path("id").asText();
            String bookingRef = entity.path("notes").path("booking_reference").asText();
            if (bookingRef.isEmpty()) {
                bookingRef = root.path("payload").path("order")
                    .path("entity").path("receipt").asText();
            }

            log.info("Razorpay webhook: event={} paymentId={} bookingRef={}",
                event, paymentId, bookingRef);

            switch (event) {
                case "payment.captured" -> bookingService.confirmBooking(bookingRef, paymentId);
                case "payment.failed"   -> log.warn("Payment failed for booking {} — scheduler will release seats", bookingRef);
                case "refund.processed" -> log.info("Refund confirmed by Razorpay for booking {}", bookingRef);
                default                 -> log.debug("Unhandled Razorpay event: {}", event);
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Error processing Razorpay webhook: {}", e.getMessage(), e);
            return ResponseEntity.ok().build(); // always ack to prevent Razorpay retry storm
        }
    }
}
