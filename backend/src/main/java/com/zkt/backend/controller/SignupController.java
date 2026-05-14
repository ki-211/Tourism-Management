package com.zkt.backend.controller;

import com.zkt.backend.common.Result;
import com.zkt.backend.entity.Signup;
import com.zkt.backend.service.SignupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/signup")
public class SignupController {

    @Autowired
    private SignupService signupService;

    @PostMapping("/{activityId}")
    public Result<?> signup(@PathVariable Long activityId, @RequestBody Signup signup) {
        try {
            // TODO: 后续可以从token中获取userId
            // Long userId = JwtUtil.getUserIdFromRequest(httpServletRequest);
            // signup.setUserId(userId);
            signup.setActivityId(activityId);
            signupService.signup(signup);
            
            if (signup.getStatus() == 1) {
                return Result.success("报名成功");
            } else {
                return Result.error("已经报名了，无需再报名");
            }
        } catch (Exception e) {
            return Result.error("报名失败：" + e.getMessage());
        }
    }

    @GetMapping("/list/{activityId}")
    public Result<List<Signup>> getList(@PathVariable Long activityId) {
        try {
            List<Signup> signups = signupService.getSignupList(activityId);
            return Result.success(signups);
        } catch (Exception e) {
            return Result.error("获取报名列表失败：" + e.getMessage());
        }
    }
}
