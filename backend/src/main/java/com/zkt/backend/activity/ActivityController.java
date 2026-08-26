package com.zkt.backend.activity;

import com.zkt.backend.auth.UserPrincipal;
import com.zkt.backend.common.ApiResponse;
import com.zkt.backend.common.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {
    private final ActivityService service;
    public ActivityController(ActivityService service) { this.service = service; }

    @GetMapping ApiResponse<PageResponse<ActivityService.ActivityView>> list(@AuthenticationPrincipal UserPrincipal p,
            @RequestParam(defaultValue = "discover") String scope, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.from(service.list(p.id(), scope, page, size)));
    }
    @PostMapping ApiResponse<ActivityService.ActivityView> create(@AuthenticationPrincipal UserPrincipal p, @Valid @RequestBody ActivityRequest r) {
        return ApiResponse.ok("活动已发布", service.create(p.id(), r.command()));
    }
    @GetMapping("/{id}") ApiResponse<ActivityService.ActivityView> detail(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id,
            @RequestParam(required = false) String invitationCode) {
        return ApiResponse.ok(service.detail(p.id(), id, invitationCode));
    }
    @GetMapping("/invitations/{code}") ApiResponse<ActivityService.InvitationPreview> invitation(@PathVariable String code) {
        return ApiResponse.ok(service.invitation(code));
    }
    @PostMapping("/{id}/signups") ApiResponse<ActivityService.ActivityView> join(@AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id, @Valid @RequestBody JoinRequest r) {
        return ApiResponse.ok("报名成功", service.join(p.id(), id, new ActivityService.JoinCommand(r.invitationCode(), r.grade(), r.passengerCount(), r.remark())));
    }
    @GetMapping("/{id}/signups") ApiResponse<List<ActivityService.MemberView>> members(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        return ApiResponse.ok(service.members(p.id(), id));
    }
    @PostMapping("/{id}/transfer") ApiResponse<ActivityService.ActivityView> transfer(@AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long id, @Valid @RequestBody TransferRequest r) {
        return ApiResponse.ok("团长已转让", service.transfer(p.id(), id, r.newCreatorId()));
    }
    @PostMapping("/{id}/invitation-code/rotate") ApiResponse<ActivityService.ActivityView> rotate(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id) {
        return ApiResponse.ok("邀请码已更新", service.rotateInvitation(p.id(), id));
    }
    @PostMapping(value = "/{id}/cover", consumes = "multipart/form-data")
    ApiResponse<ActivityService.ActivityView> cover(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long id, @RequestPart MultipartFile file) {
        return ApiResponse.ok("封面已更新", service.uploadCover(p.id(), id, file));
    }

    public record ActivityRequest(
            @NotBlank @Size(max = 100) String title,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 200) String location,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @NotNull LocalDateTime endTime, @NotNull LocalDateTime signupStart, @NotNull LocalDateTime signupEnd,
            @NotNull ActivityVisibility visibility, @Size(max = 500) String feeRule) {
        ActivityService.CreateCommand command() { return new ActivityService.CreateCommand(title, description, location, startTime, endTime, signupStart, signupEnd, visibility, feeRule); }
    }
    public record JoinRequest(@Size(max = 10) String invitationCode, @Size(max = 30) String grade,
                              @Min(1) @Max(20) Integer passengerCount, @Size(max = 500) String remark) {}
    public record TransferRequest(@NotNull Long newCreatorId) {}
}
