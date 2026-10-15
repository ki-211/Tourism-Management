package com.zkt.backend.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zkt.backend.common.DomainException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;

/**
 * Per-username brute-force protection that complements the per-IP filter: after too many failed passwords
 * within the window the username is locked until the window ends, even for attackers rotating IPs.
 * Unknown usernames are counted the same way so responses do not reveal which accounts exist.
 */
@Service
public class LoginAttemptService {
    private static final String PREFIX = "tourism:login-failures:";
    private final Cache<String, Window> local = Caffeine.newBuilder().maximumSize(100_000).expireAfterWrite(Duration.ofHours(1)).build();
    private final StringRedisTemplate redis;
    private final boolean distributed;
    private final int maxFailures;
    private final Duration window;
    private final Clock clock;

    public LoginAttemptService(StringRedisTemplate redis, Clock clock,
            @Value("${app.realtime.mode:local}") String mode,
            @Value("${app.login.max-failures:10}") int maxFailures,
            @Value("${app.login.lock-duration:PT15M}") Duration window) {
        this.redis = redis;
        this.clock = clock;
        this.distributed = "redis".equalsIgnoreCase(mode);
        this.maxFailures = maxFailures;
        this.window = window;
    }

    /** Throws 429 while the username is locked; checked before the (expensive) password hash comparison. */
    public void requireNotLocked(String username) {
        long remaining = lockedSeconds(username);
        if (remaining > 0) throw new DomainException(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_LOCKED",
                "密码错误次数过多，请 " + Math.max(1, (remaining + 59) / 60) + " 分钟后再试");
    }

    public void recordFailure(String username) {
        if (distributed) {
            String key = PREFIX + username;
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) redis.expire(key, window);
            return;
        }
        long now = clock.millis();
        local.asMap().compute(username, (ignored, old) -> old == null || now - old.startedAt >= window.toMillis()
                ? new Window(now, 1) : new Window(old.startedAt, old.failures + 1));
    }

    public void reset(String username) {
        if (distributed) redis.delete(PREFIX + username); else local.invalidate(username);
    }

    private long lockedSeconds(String username) {
        if (distributed) {
            String value = redis.opsForValue().get(PREFIX + username);
            if (value == null || Long.parseLong(value) < maxFailures) return 0;
            Long ttl = redis.getExpire(PREFIX + username);
            return ttl == null || ttl < 0 ? window.toSeconds() : ttl;
        }
        Window w = local.getIfPresent(username);
        if (w == null || w.failures < maxFailures) return 0;
        long remainingMillis = w.startedAt + window.toMillis() - clock.millis();
        return remainingMillis <= 0 ? 0 : (remainingMillis + 999) / 1000;
    }

    private record Window(long startedAt, int failures) {}
}
