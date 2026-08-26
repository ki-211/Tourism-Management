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
    public UserController(UserRepository users) { this.users = users; }

    @GetMapping ApiResponse<AuthService.UserView> me(@AuthenticationPrincipal UserPrincipal p) {
        return ApiResponse.ok(AuthService.UserView.from(find(p.id())));
    }

    @PatchMapping @Transactional
    ApiResponse<AuthService.UserView> update(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody UpdateProfile r) {
        User user = find(p.id()); user.setNickname(r.nickname().trim());
        return ApiResponse.ok("资料已更新", AuthService.UserView.from(user));
    }

    private User find(Long id) { return users.findById(id).orElseThrow(() -> DomainException.notFound("用户不存在")); }
    public record UpdateProfile(@NotBlank @Size(max = 30) String nickname) {}
}
