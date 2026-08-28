package com.zkt.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zkt.backend.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private final Cache<String, Window> local = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(2)).maximumSize(50_000).build();
    private final ObjectMapper mapper;
    private final StringRedisTemplate redis;
    private final boolean distributed;
    private final boolean enabled;
    private final int authLimit;
    private final int uploadLimit;

    public AuthRateLimitFilter(ObjectMapper mapper, StringRedisTemplate redis,
            @Value("${app.realtime.mode:local}") String mode,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.auth-per-minute:60}") int authLimit,
            @Value("${app.rate-limit.upload-per-minute:60}") int uploadLimit) {
        this.mapper = mapper;
        this.redis = redis;
        this.distributed = "redis".equalsIgnoreCase(mode);
        this.enabled = enabled;
        this.authLimit = authLimit;
        this.uploadLimit = uploadLimit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !"POST".equals(request.getMethod()) || endpointGroup(request.getRequestURI()) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String group = endpointGroup(request.getRequestURI());
        int limit = "auth".equals(group) ? authLimit : uploadLimit;
        String key = request.getRemoteAddr() + ':' + group;
        long count = distributed ? incrementDistributed(key) : incrementLocal(key);
        if (count > limit) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
            mapper.writeValue(response.getOutputStream(), ApiResponse.error("RATE_LIMITED", "请求过于频繁，请一分钟后重试"));
            return;
        }
        chain.doFilter(request, response);
    }

    private long incrementDistributed(String key) {
        String redisKey = "tourism:rate:" + key;
        Long count = redis.opsForValue().increment(redisKey);
        if (count != null && count == 1) redis.expire(redisKey, WINDOW);
        return count == null ? 1 : count;
    }

    private long incrementLocal(String key) {
        long now = Instant.now().getEpochSecond();
        return local.asMap().compute(key, (ignored, old) -> old == null || now - old.startedAt >= WINDOW.toSeconds()
                ? new Window(now, 1) : new Window(old.startedAt, old.count + 1)).count;
    }

    private String endpointGroup(String path) {
        if (path.endsWith("/auth/login") || path.endsWith("/auth/register")) return "auth";
        if (path.endsWith("/cover") || path.endsWith("/photos") || path.matches(".*/sign-tasks/\\d+/records$")) return "upload";
        return null;
    }

    private record Window(long startedAt, int count) {}
}
