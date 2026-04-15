package com.cinemax.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "offers")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Offer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String offerCode;
    private String description;
    @Enumerated(EnumType.STRING) private OfferType offerType;
    private BigDecimal discountPercentage;
    private BigDecimal maxDiscountAmount;
    private String applicableCities;
    private String applicableTheatreIds;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Boolean isActive;
}
