package com.zkt.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlbumPhoto {
    private Long id;
    private Long activityId;
    private Long userId;
    private String url;
    private LocalDateTime createTime;
}
