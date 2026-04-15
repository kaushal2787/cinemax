package com.cinemax.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;

@Entity
@Table(name = "booking_items")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BookingItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id") private Booking booking;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_seat_id") private ShowSeat showSeat;
    private BigDecimal price;
    private BigDecimal discountApplied;
    private String discountReason;
}
