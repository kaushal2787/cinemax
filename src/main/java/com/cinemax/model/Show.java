package com.cinemax.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "shows")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Show {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false) private Movie movie;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false) private Screen screen;
    @Column(nullable = false) private LocalDateTime showTime;
    @Column(nullable = false) private LocalDateTime showEndTime;
    @Enumerated(EnumType.STRING) private ShowStatus status;
    private BigDecimal silverPrice;
    private BigDecimal goldPrice;
    private BigDecimal platinumPrice;
    private BigDecimal reclinePrice;
    private Integer availableSeats;
    private Integer totalSeats;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;
}