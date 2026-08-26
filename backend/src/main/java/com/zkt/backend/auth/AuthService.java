package com.zkt.backend.auth;

import com.zkt.backend.common.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AppSecurityProperties properties;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder encoder,
                       JwtService jwt, AppSecurityProperties properties) {
        this.users = users; this.refreshTokens = refreshTokens; this.encoder = encoder; this.jwt = jwt; this.properties = properties;
    }

    @Transactional
    public User register(String username, String password, String nickname) {
        if (users.existsByUsername(username)) throw DomainException.conflict("USERNAME_EXISTS", "用户名已存在");
        User user = new User(); user.setUsername(username); user.setPasswordHash(encoder.encode(password));
        user.setNickname(nickname == null || nickname.isBlank() ? username : nickname.trim()); user.setRole("USER");
        return users.save(user);
    }

    @Transactional
    public Tokens login(String username, String password) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "用户名或密码错误"));
        if (!encoder.matches(password, user.getPasswordHash()))
            throw new DomainException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "用户名或密码错误");
        return issue(user);
    }

    @Transactional
    public Tokens refresh(String rawToken) {
        String hash = hash(rawToken);
        RefreshToken old = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "刷新令牌无效"));
        if (old.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokens.delete(old);
            throw new DomainException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "登录状态已过期");
        }
        User user = users.findById(old.getUserId()).orElseThrow(() -> DomainException.notFound("用户不存在"));
        refreshTokens.delete(old);
        return issue(user);
    }

    @Transactional public void logout(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) refreshTokens.deleteByTokenHash(hash(rawToken));
    }

    private Tokens issue(User user) {
        byte[] bytes = new byte[48]; random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken entity = new RefreshToken(); entity.setUserId(user.getId()); entity.setTokenHash(hash(raw));
        entity.setExpiresAt(LocalDateTime.now().plus(properties.refreshTtl())); refreshTokens.save(entity);
        return new Tokens(jwt.createAccessToken(user), raw, jwt.accessExpiresInSeconds(), UserView.from(user));
    }

    private String hash(String raw) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    @Scheduled(cron = "0 0 * * * *") @Transactional
    public void removeExpired() { refreshTokens.deleteByExpiresAtBefore(LocalDateTime.now()); }

    public record Tokens(String accessToken, String refreshToken, long expiresIn, UserView user) {}
    public record UserView(Long id, String username, String nickname, String role) {
        public static UserView from(User u) { return new UserView(u.getId(), u.getUsername(), u.getNickname(), u.getRole()); }
    }
}
