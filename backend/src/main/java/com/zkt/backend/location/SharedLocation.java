package com.zkt.backend.location;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "shared_locations", uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id", "user_id"}))
public class SharedLocation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "activity_id", nullable = false) private Long activityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, precision = 10, scale = 7) private BigDecimal latitude;
    @Column(nullable = false, precision = 10, scale = 7) private BigDecimal longitude;
    @Column(length = 500) private String address;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @Column(name = "expires_at", nullable = false) private LocalDateTime expiresAt;
    @PrePersist @PreUpdate void updateTime() { updatedAt = LocalDateTime.now(); }
}
