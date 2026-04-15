package com.cinemax.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "seats")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Seat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false) private Screen screen;
    @Column(nullable = false) private String seatNumber;
    @Column(nullable = false) private String row;
    @Column(nullable = false) private Integer seatIndex;
    @Enumerated(EnumType.STRING) private SeatCategory category;
}
