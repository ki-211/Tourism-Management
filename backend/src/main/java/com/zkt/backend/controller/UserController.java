package com.zkt.backend.controller;

import com.zkt.backend.common.Result;
import com.zkt.backend.entity.User;
import com.zkt.backend.service.UserService;
import com.zkt.backend.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<?> register(@RequestBody User user) {
        try {
            userService.register(user);
            return Result.success("注册成功");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("注册失败：" + e.getMessage());
        }
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body) {
        try {
            User result = userService.login(body.get("username"), body.get("password"));
            String token = JwtUtil.generateToken(result.getId());

            // 构建返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", result.getId());
            data.put("username", result.getUsername());

            return Result.success("登录成功", data);
        } catch (IllegalArgumentException e) {
            return Result.error(401, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "登录失败：" + e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result<?> getInfo(@RequestParam Long userId) {
        try {
            User user = userService.selectById(userId);
            // 不返回密码
            user.setPassword(null);
            return Result.success(user);
        } catch (Exception e) {
            return Result.error("获取用户信息失败：" + e.getMessage());
        }
    }

    @PostMapping("/update")
    public Result<?> updateNickname(@RequestBody Map<String, Object> data) {
        try {
            Long userId = Long.valueOf(data.get("userId").toString());
            String nickname = data.get("nickname").toString();
            boolean success = userService.updateNickname(userId, nickname);
            return success ? Result.success("更新成功") : Result.error("更新失败");
        } catch (Exception e) {
            return Result.error("更新失败：" + e.getMessage());
        }
    }
}

