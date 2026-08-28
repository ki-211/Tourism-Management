package com.zkt.backend.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class WebSocketTicketService {
    private static final Duration TTL = Duration.ofSeconds(60);
    private static final String PREFIX = "tourism:ws-ticket:";
    private final JwtService jwt;
    private final UserRepository users;
    private final StringRedisTemplate redis;
    private final boolean distributed;
    private final Cache<String, Long> local = Caffeine.newBuilder().expireAfterWrite(TTL).maximumSize(20_000).build();

    public WebSocketTicketService(JwtService jwt, UserRepository users, StringRedisTemplate redis,
            @Value("${app.realtime.mode:local}") String mode) {
        this.jwt = jwt;
        this.users = users;
        this.redis = redis;
        this.distributed = "redis".equalsIgnoreCase(mode);
    }

    public IssuedTicket issue(Long userId) {
        User user = users.findById(userId).filter(candidate -> candidate.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("User is not active"));
        String ticketId = UUID.randomUUID().toString();
        if (distributed) redis.opsForValue().set(PREFIX + ticketId, userId.toString(), TTL);
        else local.put(ticketId, userId);
        return new IssuedTicket(jwt.createWebSocketTicket(userId, user.getTokenVersion(), ticketId), TTL.toSeconds());
    }

    public Long consume(String rawTicket) {
        JwtService.WebSocketClaims claims = jwt.parseWebSocketTicket(rawTicket);
        Long stored;
        if (distributed) {
            String value = redis.opsForValue().getAndDelete(PREFIX + claims.ticketId());
            stored = value == null ? null : Long.valueOf(value);
        } else {
            stored = local.asMap().remove(claims.ticketId());
        }
        User user = users.findById(claims.userId()).orElse(null);
        if (!claims.userId().equals(stored) || user == null || user.getDeletedAt() != null
                || user.getTokenVersion() != claims.tokenVersion())
            throw new IllegalArgumentException("Websocket ticket already used, revoked or expired");
        return stored;
    }

    public record IssuedTicket(String ticket, long expiresIn) {}
}
