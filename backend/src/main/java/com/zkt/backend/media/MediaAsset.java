package com.zkt.backend.media;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "media_assets")
public class MediaAsset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "owner_id", nullable = false) private Long ownerId;
    @Column(name = "object_key", nullable = false, unique = true) private String objectKey;
    @Column(name = "original_name", nullable = false) private String originalName;
    @Column(name = "content_type", nullable = false, length = 50) private String contentType;
    @Column(name = "size_bytes", nullable = false) private Long sizeBytes;
    @Column(nullable = false, length = 30) private String purpose;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void create() { createdAt = LocalDateTime.now(); }
}
