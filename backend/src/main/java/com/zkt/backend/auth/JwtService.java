package com.zkt.backend.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {
    private final AppSecurityProperties properties;
    private final SecretKey key;

    public JwtService(AppSecurityProperties properties) {
        this.properties = properties;
        String secret = properties.secret();
        if (secret == null || secret.isBlank()) {
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            secret = Base64.getEncoder().encodeToString(random);
        }
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.getId().toString())
                .claim("username", user.getUsername()).claim("role", user.getRole()).claim("version", user.getTokenVersion()).claim("kind", "access")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(properties.accessTtl())))
                .signWith(key).compact();
    }

    public AccessClaims parseAccessToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!"access".equals(claims.get("kind", String.class))) throw new IllegalArgumentException("Not an access token");
        Number version = claims.get("version", Number.class);
        return new AccessClaims(Long.valueOf(claims.getSubject()), version == null ? 0 : version.longValue());
    }

    public String createWebSocketTicket(Long userId, long tokenVersion, String ticketId) {
        Instant now = Instant.now();
        return Jwts.builder().subject(userId.toString()).id(ticketId).claim("version", tokenVersion).claim("kind", "ws")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(60))).signWith(key).compact();
    }

    public WebSocketClaims parseWebSocketTicket(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!"ws".equals(claims.get("kind", String.class))) throw new IllegalArgumentException("Not a websocket ticket");
        if (claims.getId() == null || claims.getId().isBlank()) throw new IllegalArgumentException("Missing websocket ticket id");
        Number version = claims.get("version", Number.class);
        return new WebSocketClaims(Long.valueOf(claims.getSubject()), version == null ? 0 : version.longValue(), claims.getId());
    }

    public String createMediaToken(Long userId, Long mediaId) {
        Instant now = Instant.now();
        return Jwts.builder().subject(userId.toString()).claim("kind", "media").claim("mediaId", mediaId)
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(300))).signWith(key).compact();
    }

    public MediaClaims parseMediaToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!"media".equals(claims.get("kind", String.class))) throw new IllegalArgumentException("Not a media token");
        Number mediaId = claims.get("mediaId", Number.class);
        return new MediaClaims(Long.valueOf(claims.getSubject()), mediaId.longValue());
    }

    public long accessExpiresInSeconds() { return properties.accessTtl().toSeconds(); }
    public record WebSocketClaims(Long userId, long tokenVersion, String ticketId) {}
    public record MediaClaims(Long userId, Long mediaId) {}
    public record AccessClaims(Long userId, long tokenVersion) {}
}
