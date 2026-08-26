package com.zkt.backend.vehicle;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name="vehicles")
public class Vehicle {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="activity_id",nullable=false) private Long activityId;
    @Column(name="plate_number",nullable=false,length=30) private String plateNumber;
    @Column(name="driver_name",nullable=false,length=50) private String driverName;
    @Column(name="pickup_time",nullable=false) private LocalDateTime pickupTime;
    @Column(name="pickup_location",nullable=false,length=200) private String pickupLocation;
    @Column(name="created_by",nullable=false) private Long createdBy;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @PrePersist void create(){createdAt=LocalDateTime.now();}
}
