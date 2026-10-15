package com.zkt.backend.auth;

import com.zkt.backend.common.DomainException;
import com.zkt.backend.activity.ActivityRepository;
import com.zkt.backend.activity.SignupRepository;
import com.zkt.backend.location.SharedLocationRepository;
import com.zkt.backend.room.RoomEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
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
    private final Clock clock;
    private final ActivityRepository activities;
    private final SignupRepository signups;
    private final SharedLocationRepository locations;
    private final RoomEventPublisher events;
    private final AuthenticatedUserCache userCache;
    private final TransactionTemplate transactions;
    private final LoginAttemptService loginAttempts;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder encoder,
                       JwtService jwt, AppSecurityProperties properties, Clock clock, ActivityRepository activities,
                       SignupRepository signups, SharedLocationRepository locations, RoomEventPublisher events,
                       AuthenticatedUserCache userCache, TransactionTemplate transactions,
                       LoginAttemptService loginAttempts) {
        this.users = users; this.refreshTokens = refreshTokens; this.encoder = encoder; this.jwt = jwt; this.properties = properties;
        this.clock = clock;
        this.activities = activities; this.signups = signups; this.locations = locations; this.events = events; this.userCache = userCache;
        this.transactions = transactions;
        this.loginAttempts = loginAttempts;
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
        loginAttempts.requireNotLocked(username);
        User user = users.findByUsername(username).orElse(null);
        if (user == null || !encoder.matches(password, user.getPasswordHash())) {
            loginAttempts.recordFailure(username);
            throw new DomainException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "用户名或密码错误");
        }
        loginAttempts.reset(username);
        return issue(user);
    }

    @Transactional
    public Tokens refresh(String rawToken) {
        String hash = hash(rawToken);
        RefreshToken old = refreshTokens.findByTokenHash(hash)
                .orElseThrow(() -> new DomainException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "刷新令牌无效"));
        if (old.getExpiresAt().isBefore(now())) {
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

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = activeUser(userId);
        if (!encoder.matches(currentPassword, user.getPasswordHash()))
            throw new DomainException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "当前密码错误");
        if (encoder.matches(newPassword, user.getPasswordHash()))
            throw DomainException.badRequest("PASSWORD_UNCHANGED", "新密码不能与当前密码相同");
        user.setPasswordHash(encoder.encode(newPassword));
        revokeSessions(user);
    }

    @Transactional
    public void logoutAll(Long userId) { revokeSessions(activeUser(userId)); }

    @Transactional
    public void deleteAccount(Long userId, String password) {
        User user = activeUser(userId);
        if (!encoder.matches(password, user.getPasswordHash()))
            throw new DomainException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "密码错误");
        if (activities.existsByCreatorIdAndStatus(userId, com.zkt.backend.activity.ActivityStatus.ACTIVE))
            throw DomainException.conflict("CREATOR_MUST_TRANSFER", "请先转让或取消名下所有进行中的活动，再注销账号");
        locations.deleteByUserId(userId);
        signups.deleteByUserId(userId);
        refreshTokens.deleteByUserId(userId);
        user.setUsername("d_" + Long.toString(userId, 36));
        user.setNickname("已注销用户");
        user.setPasswordHash(encoder.encode(randomToken()));
        user.setRole("DELETED");
        user.setDeletedAt(now());
        user.setTokenVersion(user.getTokenVersion() + 1);
        userCache.invalidate(userId);
        events.disconnect(userId, null);
    }

    private User activeUser(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> DomainException.notFound("用户不存在"));
        if (user.getDeletedAt() != null) throw DomainException.notFound("用户不存在");
        return user;
    }

    private void revokeSessions(User user) {
        refreshTokens.deleteByUserId(user.getId());
        user.setTokenVersion(user.getTokenVersion() + 1);
        userCache.invalidate(user.getId());
        events.disconnect(user.getId(), null);
    }

    private Tokens issue(User user) {
        String raw = randomToken();
        RefreshToken entity = new RefreshToken(); entity.setUserId(user.getId()); entity.setTokenHash(hash(raw));
        entity.setExpiresAt(now().plus(properties.refreshTtl())); refreshTokens.save(entity);
        return new Tokens(jwt.createAccessToken(user), raw, jwt.accessExpiresInSeconds(), UserView.from(user));
    }

    private String hash(String raw) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    private String randomToken() {
        byte[] bytes = new byte[48]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Scheduled(cron = "0 0 * * * *")
    @SchedulerLock(name = "removeExpiredRefreshTokens", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void removeExpired() { transactions.executeWithoutResult(status -> refreshTokens.deleteByExpiresAtBefore(now())); }

    private LocalDateTime now() { return LocalDateTime.now(clock); }

    public record Tokens(String accessToken, String refreshToken, long expiresIn, UserView user) {}
    public record UserView(Long id, String username, String nickname, String role) {
        public static UserView from(User u) { return new UserView(u.getId(), u.getUsername(), u.getNickname(), u.getRole()); }
    }
}
