package com.zkt.backend.controller;

import com.zkt.backend.common.Result;
import com.zkt.backend.entity.Activity;
import com.zkt.backend.entity.Signup;
import com.zkt.backend.service.ActivityService;
import com.zkt.backend.service.SignupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    @Autowired
    private ActivityService activityService;
    
    @Autowired
    private SignupService signupService;

    @PostMapping("/create")
    public Result<?> create(@RequestBody Activity activity) {
        try {
            // TODO: 后续可以从token中获取userId
            // Long userId = JwtUtil.getUserIdFromToken(authHeader);
            // activity.setCreatorId(userId);
            activityService.create(activity);

            // 团长（创建者）默认参加活动
            if (activity.getCreatorId() != null && activity.getId() != null) {
                Signup signup = new Signup();
                signup.setActivityId(activity.getId());
                signup.setUserId(activity.getCreatorId());
                signup.setRemark("团长");
                signupService.signup(signup);
            }

            return Result.success("发布成功");
        } catch (Exception e) {
            return Result.error("发布失败：" + e.getMessage());
        }
    }

    @GetMapping("/all")
    public Result<List<Activity>> listAll() {
        try {
            List<Activity> activities = activityService.getAll();
            return Result.success(activities);
        } catch (Exception e) {
            return Result.error("获取活动列表失败：" + e.getMessage());
        }
    }

    /**
     * 我参加的活动
     */
    @GetMapping("/my")
    public Result<List<Activity>> getMyActivities(@RequestParam Long userId) {
        try {
            List<Activity> activities = signupService.getUserActivities(userId);
            return Result.success(activities);
        } catch (Exception e) {
            return Result.error("获取我的活动失败：" + e.getMessage());
        }
    }

    /**
     * 我发布的活动
     */
    @GetMapping("/published")
    public Result<List<Activity>> getPublishedActivities(@RequestParam Long userId) {
        try {
            List<Activity> activities = activityService.getActivitiesByCreatorId(userId);
            return Result.success(activities);
        } catch (Exception e) {
            return Result.error("获取发布的活动失败：" + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<Activity> getById(@PathVariable Long id) {
        try {
            Activity activity = activityService.getById(id);
            if (activity == null) {
                return Result.error("活动不存在");
            }
            return Result.success(activity);
        } catch (Exception e) {
            return Result.error("获取活动详情失败：" + e.getMessage());
        }
    }

    /**
     * 团长转让
     */
    @PostMapping("/transfer")
    public Result<?> transferCreator(@RequestBody Map<String, Long> body) {
        Long activityId = body.get("activityId");
        Long currentUserId = body.get("currentUserId");
        Long newCreatorId = body.get("newCreatorId");
        if (activityId == null || currentUserId == null || newCreatorId == null) {
            return Result.error("参数不完整");
        }
        try {
            activityService.transferCreator(activityId, currentUserId, newCreatorId);
            return Result.success("转让成功");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("转让失败：" + e.getMessage());
        }
    }
}

