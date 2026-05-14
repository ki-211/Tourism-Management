package com.zkt.backend.service;

import com.zkt.backend.entity.User;

public interface UserService {
    void register(User user);
    User login(String username, String password);
    User selectById(Long userId);
    boolean updateNickname(Long id, String nickname);
}
