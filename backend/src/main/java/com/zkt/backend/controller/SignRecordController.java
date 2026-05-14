package com.zkt.backend.controller;

import com.zkt.backend.common.Result;
import com.zkt.backend.service.SignRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/signRecord")
public class SignRecordController {

    @Autowired
    private SignRecordService signRecordService;

    @PostMapping("/do")
    public Result<?> doSign(@RequestBody Map<String, Long> params) {
        Long taskId = params.get("taskId");
        Long userId = params.get("userId");

        if (taskId == null || userId == null) {
            return Result.error("任务ID或用户ID不能为空");
        }

        try {
            signRecordService.doSign(taskId, userId);
            return Result.success("签到成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("签到失败：" + e.getMessage());
        }
    }

    @GetMapping("/listByUser")
    public Result<List<Map<String, Object>>> listByUser(@RequestParam Long userId) {
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }
        try {
            List<Map<String, Object>> list = signRecordService.listByUser(userId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("获取签到记录失败：" + e.getMessage());
        }
    }

    @GetMapping("/listByActivity")
    public Result<List<Map<String, Object>>> listByActivity(@RequestParam Long activityId) {
        if (activityId == null) {
            return Result.error("活动ID不能为空");
        }
        try {
            List<Map<String, Object>> list = signRecordService.listByActivity(activityId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("获取活动签到情况失败：" + e.getMessage());
        }
    }

    @GetMapping("/statusByActivity")
    public Result<List<Map<String, Object>>> statusByActivity(@RequestParam Long activityId) {
        if (activityId == null) {
            return Result.error("活动ID不能为空");
        }
        try {
            List<Map<String, Object>> list = signRecordService.statusByActivity(activityId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("获取活动逐任务签到状态失败：" + e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam Long taskId) {
        if (taskId == null) {
            return Result.error("任务ID不能为空");
        }
        try {
            List<Map<String, Object>> list = signRecordService.listByTask(taskId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("获取签到记录失败：" + e.getMessage());
        }
    }

    @PostMapping("/sign")
    public Result<?> sign(@RequestBody Map<String, Long> params) {
        return doSign(params);
    }

    @GetMapping("/taskDetail")
    public Result<Map<String, Object>> getTaskDetail(@RequestParam(required = false) Long taskId) {
        if (taskId == null) {
            return Result.error("任务ID不能为空");
        }
        try {
            Map<String, Object> detail = signRecordService.getTaskDetail(taskId);
            return Result.success(detail);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取签到任务详情失败：" + e.getMessage());
        }
    }
}

