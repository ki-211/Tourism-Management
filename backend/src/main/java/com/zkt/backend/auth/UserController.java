package com.zkt.backend.auth;

import com.zkt.backend.common.ApiResponse;
import com.zkt.backend.common.DomainException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final UserRepository users;
    private final AuthService auth;
    public UserController(UserRepository users, AuthService auth) { this.users = users; this.auth = auth; }

    @GetMapping ApiResponse<AuthService.UserView> me(@AuthenticationPrincipal UserPrincipal p) {
        return ApiResponse.ok(AuthService.UserView.from(find(p.id())));
    }

    @PatchMapping @Transactional
    ApiResponse<AuthService.UserView> update(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody UpdateProfile r) {
        User user = find(p.id()); user.setNickname(r.nickname().trim());
        return ApiResponse.ok("资料已更新", AuthService.UserView.from(user));
    }

    @PutMapping("/password")
    ApiResponse<Void> changePassword(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ChangePassword r) {
        auth.changePassword(p.id(), r.currentPassword(), r.newPassword());
        return ApiResponse.ok("密码已修改，请重新登录", null);
    }

    @PostMapping("/logout-all")
    ApiResponse<Void> logoutAll(@AuthenticationPrincipal UserPrincipal p) {
        auth.logoutAll(p.id());
        return ApiResponse.ok("所有设备已退出", null);
    }

    @DeleteMapping
    ApiResponse<Void> deleteAccount(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody DeleteAccount r) {
        auth.deleteAccount(p.id(), r.password());
        return ApiResponse.ok("账号已注销", null);
    }

    private User find(Long id) { return users.findById(id).orElseThrow(() -> DomainException.notFound("用户不存在")); }
    public record UpdateProfile(@NotBlank @Size(max = 30) String nickname) {}
    public record ChangePassword(@NotBlank String currentPassword, @NotBlank @Size(min = 8, max = 64) String newPassword) {}
    public record DeleteAccount(@NotBlank String password) {}
}
