package com.zkt.backend.auth;

import com.zkt.backend.common.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LoginAttemptServiceTest {
    @Test void localLockExpiresWithTheWindow() {
        MutableClock clock = new MutableClock();
        LoginAttemptService service = new LoginAttemptService(mock(StringRedisTemplate.class), clock, "local", 3, Duration.ofMinutes(15));
        for (int i = 0; i < 3; i++) service.recordFailure("alice");
        assertThatThrownBy(() -> service.requireNotLocked("alice")).isInstanceOf(DomainException.class).hasMessageContaining("15 分钟");
        clock.advance(Duration.ofMinutes(15));
        assertThatCode(() -> service.requireNotLocked("alice")).doesNotThrowAnyException();
    }

    @SuppressWarnings("unchecked")
    @Test void redisCountsAcrossInstancesAndReportsRemainingTime() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        LoginAttemptService service = new LoginAttemptService(redis, Clock.systemUTC(), "redis", 3, Duration.ofMinutes(15));

        when(values.increment("tourism:login-failures:bob")).thenReturn(1L);
        service.recordFailure("bob");
        verify(redis).expire("tourism:login-failures:bob", Duration.ofMinutes(15));

        when(values.get("tourism:login-failures:bob")).thenReturn("3");
        when(redis.getExpire("tourism:login-failures:bob")).thenReturn(90L);
        assertThatThrownBy(() -> service.requireNotLocked("bob")).hasMessageContaining("2 分钟");

        service.reset("bob");
        verify(redis).delete("tourism:login-failures:bob");
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");
        void advance(Duration duration) { now = now.plus(duration); }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
