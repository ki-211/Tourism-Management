package com.zkt.backend.attendance;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name="sign_records",uniqueConstraints=@UniqueConstraint(columnNames={"task_id","user_id"}))
public class SignRecord {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="task_id",nullable=false) private Long taskId;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(precision=10,scale=7) private BigDecimal latitude;
    @Column(precision=10,scale=7) private BigDecimal longitude;
    @Column(length=500) private String address;
    @Column(length=200) private String remark;
    @Column(name="photo_media_id") private Long photoMediaId;
    @Column(name="signed_at",nullable=false) private LocalDateTime signedAt;
    @PrePersist void create(){signedAt=LocalDateTime.now();}
}
