package com.zkt.backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Signup {
    private Long id;

    private Long activityId;

    private Long userId;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime signupTime;
    //备注
    private String remark;

    private String grade;
    //乘车人数
    private Integer passengerCount;

    //用户信息（非数据库字段，查询时关联注入）
    private String nickname;
    private String avatarUrl;
}
