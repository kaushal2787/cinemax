package com.cinemax.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "bookings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String bookingReference;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false) private Show show;
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL) private List<BookingItem> bookingItems;
    private Integer totalTickets;
    private BigDecimal baseAmount;
    private BigDecimal discountAmount;
    private BigDecimal convenienceFee;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    @Enumerated(EnumType.STRING) private BookingStatus status;
    private String paymentId;
    private String paymentGateway;
    @Enumerated(EnumType.STRING) private PaymentStatus paymentStatus;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;
}
