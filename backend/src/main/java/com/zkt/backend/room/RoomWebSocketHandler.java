package com.zkt.backend.room;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkt.backend.activity.ActivityService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class RoomWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper;
    private final ActivityService activities;
    private final RoomEventPublisher events;
    public RoomWebSocketHandler(ObjectMapper mapper, ActivityService activities, RoomEventPublisher events) {
        this.mapper = mapper; this.activities = activities; this.events = events;
    }
    @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode body = mapper.readTree(message.getPayload()); String type = body.path("type").asText();
        if ("PING".equals(type)) { session.sendMessage(new TextMessage("{\"type\":\"PONG\"}")); return; }
        if ("SUBSCRIBE_ACTIVITY".equals(type)) {
            Long userId = (Long) session.getAttributes().get("userId"); long activityId = body.path("activityId").asLong();
            activities.requireMember(activityId, userId); events.subscribe(activityId, session);
            session.sendMessage(new TextMessage("{\"type\":\"SUBSCRIBED\",\"activityId\":" + activityId + "}"));
        }
    }
    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { events.unsubscribe(session); }
    @Override public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception { events.unsubscribe(session); session.close(); }
}
