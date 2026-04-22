package com.cinemax.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "movies")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Movie {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String title;
    private String description;
    private String director;

    @Column(name = "movie_cast")
    private String movieCast;
    private Integer durationMinutes;
    private String language;
    private String genre;
    private String posterUrl;
    private String trailerUrl;
    private String certification;
    @Enumerated(EnumType.STRING) private MovieStatus status;
    private LocalDateTime releaseDate;
    @CreationTimestamp private LocalDateTime createdAt;
}
