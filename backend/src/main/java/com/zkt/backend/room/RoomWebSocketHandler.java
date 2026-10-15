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
    @Override public void afterConnectionEstablished(WebSocketSession session) { events.register(session); }
    @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode body = mapper.readTree(message.getPayload()); String type = body.path("type").asText();
        if ("PING".equals(type)) { events.reply(session, "{\"type\":\"PONG\"}"); return; }
        if ("SUBSCRIBE_ACTIVITY".equals(type)) {
            Long userId = (Long) session.getAttributes().get("userId"); long activityId = body.path("activityId").asLong();
            if (!activities.isMember(activityId, userId)) { events.unregister(session); session.close(RoomEventPublisher.ACCESS_REVOKED); return; }
            events.subscribe(activityId, session);
            events.reply(session, "{\"type\":\"SUBSCRIBED\",\"activityId\":" + activityId + "}");
        }
    }
    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { events.unregister(session); }
    @Override public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception { events.unregister(session); session.close(); }
}
