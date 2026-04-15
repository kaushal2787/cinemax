package com.cinemax.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "theatres")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Theatre {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String city;
    private String state;
    private String country;
    private String address;
    private String pincode;
    private String contactEmail;
    private String contactPhone;
    private Double rating;
    @Enumerated(EnumType.STRING) private TheatreStatus status;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id") private TheatrePartner partner;
    @OneToMany(mappedBy = "theatre", cascade = CascadeType.ALL) private List<Screen> screens;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;
}
