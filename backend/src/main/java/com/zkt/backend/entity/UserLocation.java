package com.zkt.backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserLocation {
    private Long id;
    private Long activityId;
    private Long userId;
    private Double latitude;
    private Double longitude;
    private String address;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // 关联字段
    private String nickname;
}
