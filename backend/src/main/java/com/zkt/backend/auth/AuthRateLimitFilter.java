package com.zkt.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkt.backend.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final int LIMIT = 20;
    private static final long WINDOW_SECONDS = 60;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final ObjectMapper mapper;

    public AuthRateLimitFilter(ObjectMapper mapper) { this.mapper = mapper; }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean protectedPost = path.endsWith("/auth/login") || path.endsWith("/auth/register")
                || path.endsWith("/cover") || path.endsWith("/photos") || path.matches(".*/sign-tasks/\\d+/records$");
        return !"POST".equals(request.getMethod()) || !protectedPost;
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = Instant.now().getEpochSecond();
        String key = request.getRemoteAddr() + ':' + request.getRequestURI();
        Window current = windows.compute(key, (ignored, old) -> old == null || now - old.startedAt >= WINDOW_SECONDS
                ? new Window(now, 1) : new Window(old.startedAt, old.count + 1));
        if (current.count > LIMIT) {
            response.setStatus(429); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), ApiResponse.error("RATE_LIMITED", "请求过于频繁，请一分钟后重试"));
            return;
        }
        chain.doFilter(request, response);
    }
    private record Window(long startedAt, int count) {}
}
