package com.zkt.backend.attendance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name="sign_tasks")
public class SignTask {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="activity_id",nullable=false) private Long activityId;
    @Column(nullable=false,length=100) private String title;
    @Column(length=500) private String description;
    @Column(name="created_by",nullable=false) private Long createdBy;
    private LocalDateTime deadline;
    @Column(precision=10,scale=7) private BigDecimal latitude;
    @Column(precision=10,scale=7) private BigDecimal longitude;
    @Column(name="radius_meters") private Integer radiusMeters;
    @Column(name="location_name",length=200) private String locationName;
    @Column(name="photo_required",nullable=false) private boolean photoRequired;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @PrePersist void create(){createdAt=LocalDateTime.now();}
}
