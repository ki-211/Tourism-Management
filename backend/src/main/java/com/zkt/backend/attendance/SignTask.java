package com.zkt.backend.attendance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name="sign_tasks")
public class SignTask {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="activity_id",nullable=false) private Long activityId;
    @Column(nullable=false,length=100) private String title;
    @Column(length=500) private String description;
    @Column(name="created_by",nullable=false) private Long createdBy;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @PrePersist void create(){createdAt=LocalDateTime.now();}
}
