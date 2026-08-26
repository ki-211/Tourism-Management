package com.zkt.backend.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RoomEventPublisher {
    private final ObjectMapper mapper;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> subscriptions = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    public RoomEventPublisher(ObjectMapper mapper) { this.mapper = mapper; }

    public void subscribe(Long activityId, WebSocketSession session) {
        unsubscribe(session); subscriptions.computeIfAbsent(activityId, k -> ConcurrentHashMap.newKeySet()).add(session);
        session.getAttributes().put("activityId", activityId);
    }
    public void unsubscribe(WebSocketSession session) {
        Object old = session.getAttributes().remove("activityId");
        if (old instanceof Long id) {
            Set<WebSocketSession> sessions = subscriptions.get(id);
            if (sessions != null) { sessions.remove(session); if (sessions.isEmpty()) subscriptions.remove(id); }
        }
    }
    public void publish(Long activityId, String type, Object payload) {
        Set<WebSocketSession> sessions = subscriptions.get(activityId); if (sessions == null) return;
        try {
            String json = mapper.writeValueAsString(new Event(type, activityId, sequence.incrementAndGet(), Instant.now(), payload));
            for (WebSocketSession session : Set.copyOf(sessions)) {
                try { if (session.isOpen()) session.sendMessage(new TextMessage(json)); else unsubscribe(session); }
                catch (Exception e) { unsubscribe(session); }
            }
        } catch (Exception ignored) {}
    }
    public record Event(String type, Long activityId, long sequence, Instant occurredAt, Object payload) {}
}
