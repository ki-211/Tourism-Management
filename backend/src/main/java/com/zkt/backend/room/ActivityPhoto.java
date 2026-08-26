package com.zkt.backend.room;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "activity_photos")
public class ActivityPhoto {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "activity_id", nullable = false) private Long activityId;
    @Column(name = "uploader_id", nullable = false) private Long uploaderId;
    @Column(name = "media_id", nullable = false, unique = true) private Long mediaId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void create() { createdAt = LocalDateTime.now(); }
}
