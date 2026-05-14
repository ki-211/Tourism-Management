package com.zkt.backend.entity;

import lombok.Data;

import java.sql.Date;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String role;
    private Date createdAt;
    private Date updatedAt;
}
