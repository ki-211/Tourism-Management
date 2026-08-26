package com.zkt.backend.attendance;

import com.zkt.backend.auth.UserPrincipal;
import com.zkt.backend.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;

@RestController @RequestMapping("/api/v1")
public class AttendanceController {
    private final AttendanceService service;public AttendanceController(AttendanceService service){this.service=service;}
    @GetMapping("/sign-tasks/unsigned") ApiResponse<List<AttendanceService.TaskView>> unsigned(@AuthenticationPrincipal UserPrincipal p){return ApiResponse.ok(service.unsigned(p.id()));}
    @GetMapping("/activities/{activityId}/sign-tasks") ApiResponse<List<AttendanceService.TaskView>> tasks(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long activityId){return ApiResponse.ok(service.listTasks(p.id(),activityId));}
    @PostMapping("/activities/{activityId}/sign-tasks") ApiResponse<AttendanceService.TaskView> create(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long activityId,@Valid @RequestBody TaskRequest r){return ApiResponse.ok("签到任务已发布",service.createTask(p.id(),activityId,r.title(),r.description()));}
    @GetMapping("/sign-tasks/{taskId}") ApiResponse<AttendanceService.TaskView> task(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long taskId){return ApiResponse.ok(service.task(p.id(),taskId));}
    @GetMapping("/sign-tasks/{taskId}/summary") ApiResponse<AttendanceService.TaskSummary> summary(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long taskId){return ApiResponse.ok(service.summary(p.id(),taskId));}
    @PostMapping(value="/sign-tasks/{taskId}/records",consumes="multipart/form-data") ApiResponse<AttendanceService.RecordView> sign(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long taskId,@RequestParam(required=false) BigDecimal latitude,@RequestParam(required=false) BigDecimal longitude,@RequestParam(required=false) @Size(max=500) String address,@RequestParam(required=false) @Size(max=200) String remark,@RequestPart(required=false) MultipartFile file){return ApiResponse.ok("签到成功",service.sign(p.id(),taskId,latitude,longitude,address,remark,file));}
    @PostMapping(value="/sign-tasks/{taskId}/records",consumes="application/json") ApiResponse<AttendanceService.RecordView> signWithoutPhoto(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long taskId,@Valid @RequestBody SignRequest r){return ApiResponse.ok("签到成功",service.sign(p.id(),taskId,r.latitude(),r.longitude(),r.address(),r.remark(),null));}
    @GetMapping("/users/me/sign-records") ApiResponse<List<AttendanceService.HistoryView>> history(@AuthenticationPrincipal UserPrincipal p){return ApiResponse.ok(service.history(p.id()));}
    public record TaskRequest(@NotBlank @Size(max=100) String title,@Size(max=500) String description){}
    public record SignRequest(BigDecimal latitude,BigDecimal longitude,@Size(max=500) String address,@Size(max=200) String remark){}
}
