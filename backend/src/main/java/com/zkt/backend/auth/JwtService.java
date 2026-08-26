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
                .claim("username", user.getUsername()).claim("role", user.getRole()).claim("kind", "access")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(properties.accessTtl())))
                .signWith(key).compact();
    }

    public Long parseUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!"access".equals(claims.get("kind", String.class))) throw new IllegalArgumentException("Not an access token");
        return Long.valueOf(claims.getSubject());
    }

    public String createWebSocketTicket(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder().subject(userId.toString()).claim("kind", "ws")
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(60))).signWith(key).compact();
    }

    public Long parseWebSocketTicket(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!"ws".equals(claims.get("kind", String.class))) throw new IllegalArgumentException("Not a websocket ticket");
        return Long.valueOf(claims.getSubject());
    }

    public long accessExpiresInSeconds() { return properties.accessTtl().toSeconds(); }
}
