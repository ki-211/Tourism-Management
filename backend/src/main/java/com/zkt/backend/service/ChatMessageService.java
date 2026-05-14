package com.zkt.backend.service;

import com.zkt.backend.entity.ChatMessage;
import java.util.List;

public interface ChatMessageService {
    void send(ChatMessage message);
    List<ChatMessage> list(Long activityId);
}
