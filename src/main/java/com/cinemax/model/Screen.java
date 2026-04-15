package com.cinemax.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.util.List;

@Entity
@Table(name = "screens")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Screen {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theatre_id", nullable = false) private Theatre theatre;
    @Column(nullable = false) private String name;
    private Integer totalSeats;
    @Enumerated(EnumType.STRING) private ScreenType screenType;
    @OneToMany(mappedBy = "screen", cascade = CascadeType.ALL) private List<Seat> seats;
    @OneToMany(mappedBy = "screen") private List<Show> shows;
}
