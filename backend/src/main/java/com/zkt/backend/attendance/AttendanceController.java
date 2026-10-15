package com.zkt.backend.attendance;

import com.zkt.backend.auth.UserPrincipal;
import com.zkt.backend.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Validated @RestController @RequestMapping("/api/v1")
public class AttendanceController {
    private final AttendanceService service;public AttendanceController(AttendanceService service){this.service=service;}
    @GetMapping("/sign-tasks/unsigned") ApiResponse<List<AttendanceService.TaskView>> unsigned(@AuthenticationPrincipal UserPrincipal p){return ApiResponse.ok(service.unsigned(p.id()));}
    @GetMapping("/activities/{activityId}/sign-tasks") ApiResponse<List<AttendanceService.TaskView>> tasks(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long activityId){return ApiResponse.ok(service.listTasks(p.id(),activityId));}
    @PostMapping("/activities/{activityId}/sign-tasks") ApiResponse<AttendanceService.TaskView> create(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long activityId,@Valid @RequestBody TaskRequest r){return ApiResponse.ok("签到任务已发布",service.createTask(p.id(),activityId,r.command()));}
    @GetMapping("/sign-tasks/{taskId}") ApiResponse<AttendanceService.TaskView> task(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long taskId){return ApiResponse.ok(service.task(p.id(),taskId));}
    @GetMapping("/sign-tasks/{taskId}/summary") ApiResponse<AttendanceService.TaskSummary> summary(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long taskId){return ApiResponse.ok(service.summary(p.id(),taskId));}
    @PostMapping(value="/sign-tasks/{taskId}/records",consumes="multipart/form-data") ApiResponse<AttendanceService.RecordView> sign(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long taskId,@RequestParam(required=false) @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,@RequestParam(required=false) @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,@RequestParam(required=false) @Size(max=500) String address,@RequestParam(required=false) @Size(max=200) String remark,@RequestPart(required=false) MultipartFile file){return ApiResponse.ok("签到成功",service.sign(p.id(),taskId,latitude,longitude,address,remark,file));}
    @PostMapping(value="/sign-tasks/{taskId}/records",consumes="application/json") ApiResponse<AttendanceService.RecordView> signWithoutPhoto(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long taskId,@Valid @RequestBody SignRequest r){return ApiResponse.ok("签到成功",service.sign(p.id(),taskId,r.latitude(),r.longitude(),r.address(),r.remark(),null));}
    @GetMapping("/users/me/sign-records") ApiResponse<List<AttendanceService.HistoryView>> history(@AuthenticationPrincipal UserPrincipal p){return ApiResponse.ok(service.history(p.id()));}
    public record TaskRequest(@NotBlank @Size(max=100) String title, @Size(max=500) String description, LocalDateTime deadline,
                              @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude, @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
                              @Min(value=50, message="签到范围不能小于 50 米") @Max(value=5000, message="签到范围不能超过 5000 米") Integer radiusMeters,
                              @Size(max=200) String locationName, Boolean photoRequired) {
        AttendanceService.TaskCommand command() {
            return new AttendanceService.TaskCommand(title, description, deadline, latitude, longitude, radiusMeters, locationName, Boolean.TRUE.equals(photoRequired));
        }
    }
    public record SignRequest(@DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,@DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,@Size(max=500) String address,@Size(max=200) String remark){}
}
