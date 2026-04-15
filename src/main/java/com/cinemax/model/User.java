package com.cinemax.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String passwordHash;
    private String firstName;
    private String lastName;
    private String phone;
    private String city;
    @Enumerated(EnumType.STRING) private UserRole role;
    private Boolean isVerified;
    private Boolean isActive;
    @CreationTimestamp private LocalDateTime createdAt;
}
