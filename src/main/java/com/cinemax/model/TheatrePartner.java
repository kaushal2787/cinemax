package com.cinemax.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "theatre_partners")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TheatrePartner {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String businessName;
    @Column(nullable = false, unique = true) private String email;
    private String phone;
    private String gstNumber;
    private String panNumber;
    private String bankAccountNumber;
    private String ifscCode;
    @Enumerated(EnumType.STRING) private PartnerStatus status;
    @OneToMany(mappedBy = "partner") private List<Theatre> theatres;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;
}
