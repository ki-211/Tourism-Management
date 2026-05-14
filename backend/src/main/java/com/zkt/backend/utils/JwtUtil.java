package com.zkt.backend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

public class JwtUtil {

    // 尝试从环境变量获取密钥，如果没有则使用默认值（仅用于开发环境）
    private static final String SECRET_KEY = System.getenv("JWT_SECRET") != null ? 
            System.getenv("JWT_SECRET") : "eYAK/679sgdyRw7Y4CiT4yZ528fbvzEN1FTZB4Csrx4=";
    private static final long EXPIRATION_TIME = 86400000;

    private static final Key SIGNING_KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    public static String generateToken(Long userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SIGNING_KEY)
                .compact();
    }

    public static Long getUserIdFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                JwtParser parser = Jwts.parser().verifyWith((SecretKey) SIGNING_KEY).build(); //  0.12 写法
                Claims claims = parser.parseSignedClaims(token).getPayload();
                return Long.valueOf(claims.getSubject());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}
