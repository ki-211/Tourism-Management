package com.zkt.backend.activity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "activities")
public class Activity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 100) private String title;
    @Column(length = 2000) private String description;
    @Column(nullable = false, length = 200) private String location;
    @Column(name = "start_time", nullable = false) private LocalDateTime startTime;
    @Column(name = "end_time", nullable = false) private LocalDateTime endTime;
    @Column(name = "signup_start", nullable = false) private LocalDateTime signupStart;
    @Column(name = "signup_end", nullable = false) private LocalDateTime signupEnd;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ActivityVisibility visibility;
    @Column(name = "invitation_code", unique = true, length = 10) private String invitationCode;
    @Column(name = "fee_rule", length = 500) private String feeRule;
    @Column(name = "creator_id", nullable = false) private Long creatorId;
    @Column(name = "cover_media_id") private Long coverMediaId;
    @Version private Long version;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void create() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
}
