package com.zkt.backend.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class RoomEventPublisher {
    /** Close code telling clients that access was revoked and reconnecting will not help. */
    public static final CloseStatus ACCESS_REVOKED = new CloseStatus(4403, "ACCESS_REVOKED");
    static final String DISCONNECT_TYPE = "__DISCONNECT";
    private static final String ACTIVITY_ATTR = "activityId";
    private static final String USER_ATTR = "userId";
    private static final int SEND_TIME_LIMIT_MS = 5000;
    private static final int SEND_BUFFER_LIMIT_BYTES = 512 * 1024;

    private final ObjectMapper mapper;
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> subscriptions = new ConcurrentHashMap<>();
    // Keyed by session id; values are thread-safe decorators so events, PONG and close never write concurrently.
    private final ConcurrentHashMap<String, WebSocketSession> connections = new ConcurrentHashMap<>();
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

    public void register(WebSocketSession session) {
        connections.put(session.getId(), new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, SEND_BUFFER_LIMIT_BYTES));
    }
    public void unregister(WebSocketSession session) {
        unsubscribe(session);
        connections.remove(session.getId());
    }
    public void subscribe(Long activityId, WebSocketSession session) {
        unsubscribe(session);
        WebSocketSession outbound = outbound(session);
        if (outbound == null) return;
        subscriptions.compute(activityId, (ignored, sessions) -> {
            Set<WebSocketSession> target = sessions == null ? ConcurrentHashMap.newKeySet() : sessions;
            target.add(outbound);
            return target;
        });
        session.getAttributes().put(ACTIVITY_ATTR, activityId);
    }
    public void unsubscribe(WebSocketSession session) {
        Object old = session.getAttributes().remove(ACTIVITY_ATTR);
        WebSocketSession outbound = outbound(session);
        if (old instanceof Long id && outbound != null) {
            subscriptions.computeIfPresent(id, (ignored, sessions) -> {
                sessions.remove(outbound);
                return sessions.isEmpty() ? null : sessions;
            });
        }
    }
    /** Sends a direct reply (PONG, SUBSCRIBED) through the same serialized channel as broadcast events. */
    public void reply(WebSocketSession session, String json) {
        WebSocketSession outbound = outbound(session);
        if (outbound == null) return;
        try { outbound.sendMessage(new TextMessage(json)); }
        catch (Exception e) { log.debug("WebSocket reply failed sessionId={} errorType={}", session.getId(), e.getClass().getSimpleName()); }
    }

    /** Publishes after the surrounding transaction commits, so clients that refetch over HTTP see committed data. */
    public void publish(Long activityId, String type, Object payload) {
        afterCommit(() -> send(activityId, type, payload));
    }

    /**
     * Closes the user's live connections (all of them when activityId is null, otherwise only those
     * subscribed to that activity) on every instance, after the surrounding transaction commits.
     */
    public void disconnect(Long userId, Long activityId) {
        afterCommit(() -> {
            if (!distributed) { closeLocal(userId, activityId); return; }
            try { redis.convertAndSend(channel, mapper.writeValueAsString(new Event(DISCONNECT_TYPE, activityId, 0, Instant.now(), new Disconnect(userId)))); }
            catch (Exception e) { log.warn("Realtime disconnect broadcast failed userId={} errorType={}", userId, e.getClass().getSimpleName()); }
        });
    }

    public void deliverFromBroker(String json) {
        try {
            Event event = mapper.readValue(json, Event.class);
            if (DISCONNECT_TYPE.equals(event.type())) {
                Disconnect target = mapper.convertValue(event.payload(), Disconnect.class);
                closeLocal(target.userId(), event.activityId());
            } else deliver(event.activityId(), json);
        } catch (Exception e) {
            log.warn("Realtime broker message dropped errorType={}", e.getClass().getSimpleName());
        }
    }

    private void send(Long activityId, String type, Object payload) {
        try {
            long next = distributed ? redis.opsForValue().increment("tourism:room-event-sequence") : sequence.incrementAndGet();
            String json = mapper.writeValueAsString(new Event(type, activityId, next, Instant.now(), payload));
            if (distributed) redis.convertAndSend(channel, json); else deliver(activityId, json);
        } catch (Exception e) {
            log.warn("Realtime event publish failed activityId={} type={} errorType={}", activityId, type, e.getClass().getSimpleName());
        }
    }

    private void deliver(Long activityId, String json) {
        Set<WebSocketSession> sessions = subscriptions.get(activityId); if (sessions == null) return;
        for (WebSocketSession session : Set.copyOf(sessions)) {
            try { if (session.isOpen()) session.sendMessage(new TextMessage(json)); else unsubscribe(session); }
            catch (Exception e) { unsubscribe(session); }
        }
    }

    private void closeLocal(Long userId, Long activityId) {
        for (WebSocketSession session : connections.values()) {
            Map<String, Object> attributes = session.getAttributes();
            if (!userId.equals(attributes.get(USER_ATTR))) continue;
            if (activityId != null && !activityId.equals(attributes.get(ACTIVITY_ATTR))) continue;
            unsubscribe(session);
            try { session.close(ACCESS_REVOKED); } catch (Exception ignored) { /* already closed */ }
        }
    }

    private WebSocketSession outbound(WebSocketSession session) { return connections.get(session.getId()); }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { action.run(); }
            });
        } else action.run();
    }

    public record Event(String type, Long activityId, long sequence, Instant occurredAt, Object payload) {}
    public record Disconnect(Long userId) {}
}
