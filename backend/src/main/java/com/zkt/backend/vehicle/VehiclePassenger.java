package com.zkt.backend.vehicle;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "vehicle_passengers", uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id", "user_id"}))
public class VehiclePassenger {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "vehicle_id", nullable = false) private Long vehicleId;
    @Column(name = "activity_id", nullable = false) private Long activityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void create() { createdAt = LocalDateTime.now(); }
}
