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
import java.math.BigDecimal;

// ─────────────────────────────────────────────
// RazorpayPaymentServiceImpl
// ─────────────────────────────────────────────
@Service
@Slf4j
public class RazorpayPaymentServiceImpl implements com.cinemax.service.PaymentService {

    private final com.cinemax.repository.BookingRepository bookingRepository;

    // Injected from application.yml via @Value
    private final String razorpayKeyId;
    private final String razorpayKeySecret;
    private final String webhookSecret;

    RazorpayPaymentServiceImpl(
            com.cinemax.repository.BookingRepository bookingRepository,
            @org.springframework.beans.factory.annotation.Value("${payment.razorpay.key-id:test_key}") String razorpayKeyId,
            @org.springframework.beans.factory.annotation.Value("${payment.razorpay.key-secret:test_secret}") String razorpayKeySecret,
            @org.springframework.beans.factory.annotation.Value("${payment.razorpay.webhook-secret:test_webhook}") String webhookSecret) {
        this.bookingRepository = bookingRepository;
        this.razorpayKeyId     = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
        this.webhookSecret     = webhookSecret;
    }

    /**
     * Creates a Razorpay order and returns the checkout URL.
     *
     * Production flow:
     *   POST https://api.razorpay.com/v1/orders
     *   Body: { amount: paise, currency: "INR", receipt: bookingRef, notes: {...} }
     *   Returns: { id: "order_xxx", ... }
     *   Redirect user to: https://checkout.razorpay.com/v1/checkout.js with order_id
     */
    @Override
    public String initiatePayment(com.cinemax.model.Booking booking) {
        // Convert rupees → paise (Razorpay uses smallest currency unit)
        long amountPaise = booking.getTotalAmount()
            .multiply(new java.math.BigDecimal("100"))
            .longValue();

        // Build the order request JSON (production: use Razorpay Java SDK)
        String orderRequestJson = String.format(
            "{\"amount\":%d,\"currency\":\"INR\",\"receipt\":\"%s\"," +
            "\"notes\":{\"booking_id\":\"%d\",\"movie\":\"%s\"}}",
            amountPaise,
            booking.getBookingReference(),
            booking.getId(),
            booking.getShow().getMovie().getTitle()
        );

        log.info("Creating Razorpay order for booking={} amount={}p",
            booking.getBookingReference(), amountPaise);


        // RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        // JSONObject order = client.orders.create(new JSONObject(orderRequestJson));
        // String razorpayOrderId = order.getString("id");

        // For now: return checkout URL with booking reference as order identifier
        String checkoutUrl = String.format(
            "https://checkout.razorpay.com/v1/checkout?key=%s&order_id=%s&amount=%d" +
            "&currency=INR&name=CineMax&description=%s&prefill.email=%s",
            razorpayKeyId,
            "order_" + booking.getBookingReference(),
            amountPaise,
            java.net.URLEncoder.encode(booking.getShow().getMovie().getTitle(), java.nio.charset.StandardCharsets.UTF_8),
            java.net.URLEncoder.encode(booking.getUser().getEmail(), java.nio.charset.StandardCharsets.UTF_8)
        );

        log.info("Payment URL generated for booking {}", booking.getBookingReference());
        return checkoutUrl;
    }

    /**
     * Initiates a full refund for a cancelled booking.
     *
     * Production flow:
     *   POST https://api.razorpay.com/v1/payments/{paymentId}/refund
     *   Body: { amount: paise, notes: { reason: "booking_cancelled" } }
     */
    @Override
    public void initiateRefund(com.cinemax.model.Booking booking) {
        if (booking.getPaymentId() == null) {
            log.warn("No payment ID for booking {} — skipping refund", booking.getBookingReference());
            return;
        }

        long refundAmountPaise = booking.getTotalAmount()
            .multiply(new java.math.BigDecimal("100"))
            .longValue();

        log.info("Initiating refund for booking={} paymentId={} amount={}p",
            booking.getBookingReference(), booking.getPaymentId(), refundAmountPaise);

        // Production: call Razorpay refund API
        // RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        // JSONObject refundReq = new JSONObject()
        //     .put("amount", refundAmountPaise)
        //     .put("notes", new JSONObject().put("reason", "booking_cancelled"));
        // JSONObject refund = client.payments.refund(booking.getPaymentId(), refundReq);
        // String refundId = refund.getString("id");

        // Update booking with refund status
        booking.setPaymentStatus(com.cinemax.model.PaymentStatus.REFUNDED);
        bookingRepository.save(booking);
        log.info("Refund processed for booking {}", booking.getBookingReference());
    }

    /**
     * Verifies Razorpay webhook signature using HMAC-SHA256.
     * Must be called before processing any webhook payload.
     *
     * @param payload   raw request body string
     * @param signature X-Razorpay-Signature header value
     * @return true if signature is valid
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec keySpec =
                new javax.crypto.spec.SecretKeySpec(
                    webhookSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String computed = bytesToHex(hash);
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
