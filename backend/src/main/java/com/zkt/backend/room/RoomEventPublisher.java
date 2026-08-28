package com.zkt.backend.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redis;
    private final boolean distributed;
    private final String channel;
    public RoomEventPublisher(ObjectMapper mapper, StringRedisTemplate redis,
            @Value("${app.realtime.mode:local}") String mode,
            @Value("${app.realtime.channel:tourism:room-events}") String channel) {
        this.mapper = mapper;
        this.redis = redis;
        this.distributed = "redis".equalsIgnoreCase(mode);
        this.channel = channel;
    }

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
        try {
            long next = distributed ? redis.opsForValue().increment("tourism:room-event-sequence") : sequence.incrementAndGet();
            String json = mapper.writeValueAsString(new Event(type, activityId, next, Instant.now(), payload));
            if (distributed) redis.convertAndSend(channel, json); else deliver(activityId, json);
        } catch (Exception ignored) {}
    }

    public void deliverFromBroker(String json) {
        try {
            Event event = mapper.readValue(json, Event.class);
            deliver(event.activityId(), json);
        } catch (Exception ignored) {}
    }

    private void deliver(Long activityId, String json) {
        Set<WebSocketSession> sessions = subscriptions.get(activityId); if (sessions == null) return;
        for (WebSocketSession session : Set.copyOf(sessions)) {
            try { if (session.isOpen()) session.sendMessage(new TextMessage(json)); else unsubscribe(session); }
            catch (Exception e) { unsubscribe(session); }
        }
    }
    public record Event(String type, Long activityId, long sequence, Instant occurredAt, Object payload) {}
}
