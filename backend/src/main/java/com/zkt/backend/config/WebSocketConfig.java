package com.zkt.backend.config;

import com.zkt.backend.auth.JwtService;
import com.zkt.backend.room.RoomWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final RoomWebSocketHandler handler;
    private final JwtService jwt;
    private final CorsProperties cors;
    public WebSocketConfig(RoomWebSocketHandler handler, JwtService jwt, CorsProperties cors) { this.handler = handler; this.jwt = jwt; this.cors = cors; }
    @Override public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws").addInterceptors(new TicketInterceptor(jwt))
                .setAllowedOrigins(cors.allowedOrigins().toArray(String[]::new));
    }
    private static class TicketInterceptor implements HandshakeInterceptor {
        private final JwtService jwt; TicketInterceptor(JwtService jwt) { this.jwt = jwt; }
        @Override public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse res, WebSocketHandler h, Map<String,Object> attrs) {
            try {
                String ticket = UriComponentsBuilder.fromUri(req.getURI()).build().getQueryParams().getFirst("ticket");
                attrs.put("userId", jwt.parseWebSocketTicket(ticket)); return true;
            } catch (Exception e) { return false; }
        }
        @Override public void afterHandshake(ServerHttpRequest r, ServerHttpResponse s, WebSocketHandler h, Exception e) {}
    }
}
