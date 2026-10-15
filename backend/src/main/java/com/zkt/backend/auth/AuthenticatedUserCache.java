package com.zkt.backend.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zkt.backend.common.AfterCommit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Caches the fields needed to authenticate a request so that every API call does not hit the users table.
 * Revocations (password change, logout-all, account deletion) invalidate the entry after commit on every
 * instance through Redis; the short TTL bounds staleness if an invalidation message is ever lost.
 */
@Slf4j
@Component
public class AuthenticatedUserCache {
    public static final String INVALIDATION_CHANNEL = "tourism:user-invalidations";
    private static final Duration TTL = Duration.ofSeconds(60);

    private final Cache<Long, Snapshot> cache = Caffeine.newBuilder().expireAfterWrite(TTL).maximumSize(100_000).build();
    private final UserRepository users;
    private final StringRedisTemplate redis;
    private final boolean distributed;

    public AuthenticatedUserCache(UserRepository users, StringRedisTemplate redis, @Value("${app.realtime.mode:local}") String mode) {
        this.users = users;
        this.redis = redis;
        this.distributed = "redis".equalsIgnoreCase(mode);
    }

    public Optional<Snapshot> find(Long userId) {
        return Optional.ofNullable(cache.get(userId, id -> users.findById(id).map(Snapshot::from).orElse(null)));
    }

    public void invalidate(Long userId) {
        AfterCommit.run(() -> {
            cache.invalidate(userId);
            if (!distributed) return;
            try { redis.convertAndSend(INVALIDATION_CHANNEL, userId.toString()); }
            catch (Exception e) { log.warn("User cache invalidation broadcast failed userId={} errorType={}", userId, e.getClass().getSimpleName()); }
        });
    }

    public void evictFromBroker(String message) {
        try { cache.invalidate(Long.valueOf(message.trim())); }
        catch (NumberFormatException e) { log.warn("Ignored malformed user cache invalidation message"); }
    }

    public record Snapshot(Long id, String username, String role, long tokenVersion, boolean deleted) {
        static Snapshot from(User user) {
            return new Snapshot(user.getId(), user.getUsername(), user.getRole(), user.getTokenVersion(), user.getDeletedAt() != null);
        }
        public UserPrincipal principal() { return new UserPrincipal(id, username, null, role); }
    }
}
