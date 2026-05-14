package com.zkt.backend.controller;

import com.zkt.backend.common.Result;
import com.zkt.backend.entity.SignTask;
import com.zkt.backend.service.SignTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/signTask")
public class SignTaskController {

    @Autowired
    private SignTaskService signTaskService;

    @GetMapping("/unsigned")
    public Result<List<SignTask>> getUnSignedTasks(@RequestParam(required = false) Long userId) {
        try {
            if (userId == null) {
                return Result.error("用户ID不能为空");
            }
            List<SignTask> list = signTaskService.getUnSignedTasksByUser(userId);
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取未签到任务失败：" + e.getMessage());
        }
    }

    @PostMapping("/create")
    public Result<?> create(@RequestBody SignTask task) {
        try {
            signTaskService.create(task);
            return Result.success("发布成功");
        } catch (Exception e) {
            return Result.error("发布失败：" + e.getMessage());
        }
    }

    @GetMapping("/listByActivity")
    public Result<List<SignTask>> listByActivity(@RequestParam(required = false) Long activityId) {
        try {
            if (activityId == null) {
                return Result.error("活动ID不能为空");
            }
            List<SignTask> list = signTaskService.getByActivity(activityId);
            return Result.success(list);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取签到任务失败：" + e.getMessage());
        }
    }
}
