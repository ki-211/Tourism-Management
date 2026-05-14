package com.zkt.backend.service.impl;

import com.zkt.backend.entity.User;
import com.zkt.backend.mapper.UserMapper;
import com.zkt.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    // 使用BCrypt加密密码
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void register(User user) {
        Assert.isNull(userMapper.findByUsername(user.getUsername()), "用户名已存在");
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER");
        userMapper.insert(user);
    }

    @Override
    public User login(String username, String password) {
        User dbUser = userMapper.findByUsername(username);
        Assert.notNull(dbUser, "用户不存在");
        
        // 验证密码
        if (!passwordEncoder.matches(password, dbUser.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        
        return dbUser;
    }

    @Override
    public User selectById(Long userId) {
        User dbUser = userMapper.findByUserId(userId);
        Assert.notNull(dbUser, "用户不存在");
        return dbUser;
    }

    @Override
    public boolean updateNickname(Long id, String nickname) {
        return userMapper.updateNickname(id, nickname) > 0;
    }
}
