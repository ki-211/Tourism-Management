package com.zkt.backend.activity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "signups", uniqueConstraints = @UniqueConstraint(columnNames = {"activity_id", "user_id"}))
public class Signup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "activity_id", nullable = false) private Long activityId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(length = 30) private String grade;
    @Column(name = "passenger_count") private Integer passengerCount;
    @Column(length = 500) private String remark;
    @Column(name = "joined_at", nullable = false) private LocalDateTime joinedAt;
    @PrePersist void create() { joinedAt = LocalDateTime.now(); }
}
