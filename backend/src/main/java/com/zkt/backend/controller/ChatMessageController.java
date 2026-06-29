package com.zkt.backend.controller;

import com.zkt.backend.common.Result;
import com.zkt.backend.entity.ChatMessage;
import com.zkt.backend.service.ChatMessageService;
import com.zkt.backend.service.SignupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatMessageController {

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private SignupService signupService;

    @GetMapping("/list")
    public Result<List<ChatMessage>> list(@RequestParam(required = false) Long activityId,
                                          @RequestParam(required = false) Long userId) {
        try {
            if (activityId == null) {
                return Result.error("活动ID不能为空");
            }
            if (userId == null) {
                return Result.error("用户ID不能为空");
            }

            // 验证用户是否已报名该活动
            if (!signupService.hasUserSignedUp(activityId, userId)) {
                return Result.error("您还未报名该活动，无法访问活动室");
            }

            return Result.success(chatMessageService.list(activityId));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取消息列表失败：" + e.getMessage());
        }
    }

    @PostMapping("/send")
    public Result<?> send(@RequestBody ChatMessage message) {
        try {
            if (message.getActivityId() == null) {
                return Result.error("活动ID不能为空");
            }
            if (message.getUserId() == null) {
                return Result.error("用户ID不能为空");
            }
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                return Result.error("消息内容不能为空");
            }

            // 验证用户是否已报名该活动
            if (!signupService.hasUserSignedUp(message.getActivityId(), message.getUserId())) {
                return Result.error("您还未报名该活动，无法发送消息");
            }

            chatMessageService.send(message);
            return Result.success("发送成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("发送失败: " + e.getMessage());
        }
    }
}
