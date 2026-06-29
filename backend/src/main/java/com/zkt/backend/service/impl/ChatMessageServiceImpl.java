package com.zkt.backend.service.impl;

import com.zkt.backend.entity.ChatMessage;
import com.zkt.backend.mapper.ChatMessageMapper;
import com.zkt.backend.service.ChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatMessageServiceImpl implements ChatMessageService {
    @Autowired
    private ChatMessageMapper chatMessageMapper;

    @Override
    public void send(ChatMessage message) {
        chatMessageMapper.insert(message);
    }

    @Override
    public List<ChatMessage> list(Long activityId) {
        return chatMessageMapper.findByActivityId(activityId);
    }
}
