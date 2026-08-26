package com.zkt.backend.auth;

import com.zkt.backend.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    private final JwtService jwt;
    public AuthController(AuthService service, JwtService jwt) { this.service = service; this.jwt = jwt; }

    @PostMapping("/register")
    ApiResponse<AuthService.UserView> register(@Valid @RequestBody RegisterRequest r) {
        return ApiResponse.ok("注册成功", AuthService.UserView.from(service.register(r.username(), r.password(), r.nickname())));
    }
    @PostMapping("/login") ApiResponse<AuthService.Tokens> login(@Valid @RequestBody LoginRequest r) {
        return ApiResponse.ok("登录成功", service.login(r.username(), r.password()));
    }
    @PostMapping("/refresh") ApiResponse<AuthService.Tokens> refresh(@Valid @RequestBody TokenRequest r) {
        return ApiResponse.ok(service.refresh(r.refreshToken()));
    }
    @PostMapping("/logout") ApiResponse<Void> logout(@Valid @RequestBody TokenRequest r) {
        service.logout(r.refreshToken()); return ApiResponse.ok("已退出登录", null);
    }
    @PostMapping("/ws-ticket") ApiResponse<WsTicket> wsTicket(@AuthenticationPrincipal UserPrincipal p) {
        return ApiResponse.ok(new WsTicket(jwt.createWebSocketTicket(p.id()), 60));
    }

    public record RegisterRequest(
            @NotBlank @Pattern(regexp = "^[A-Za-z0-9_]{3,16}$", message = "须为3-16位字母、数字或下划线") String username,
            @NotBlank @Size(min = 8, max = 64) String password,
            @Size(max = 30) String nickname) {}
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record TokenRequest(@NotBlank String refreshToken) {}
    public record WsTicket(String ticket, long expiresIn) {}
}
