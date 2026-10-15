package com.zkt.backend.vehicle;

import com.zkt.backend.auth.UserPrincipal;
import com.zkt.backend.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/activities/{activityId}/vehicles")
public class VehicleController {
    private final VehicleService service;
    public VehicleController(VehicleService service) { this.service = service; }

    @GetMapping ApiResponse<List<VehicleService.VehicleView>> list(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long activityId) {
        return ApiResponse.ok(service.list(p.id(), activityId));
    }
    @PostMapping ApiResponse<VehicleService.VehicleView> create(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long activityId,
            @Valid @RequestBody Request r) {
        return ApiResponse.ok("车辆已发布", service.create(p.id(), activityId, r.command()));
    }
    @PutMapping("/{vehicleId}") ApiResponse<VehicleService.VehicleView> update(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long activityId,
            @PathVariable Long vehicleId, @Valid @RequestBody Request r) {
        return ApiResponse.ok("车辆已更新", service.update(p.id(), activityId, vehicleId, r.command()));
    }
    @DeleteMapping("/{vehicleId}") ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal p, @PathVariable Long activityId,
            @PathVariable Long vehicleId) {
        service.delete(p.id(), activityId, vehicleId);
        return ApiResponse.ok("车辆已删除", null);
    }
    /** Without a body (or userId) the caller boards; the leader may pass another member's userId. */
    @PostMapping("/{vehicleId}/passengers") ApiResponse<VehicleService.VehicleView> board(@AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long activityId, @PathVariable Long vehicleId, @RequestBody(required = false) BoardRequest r) {
        return ApiResponse.ok("已安排座位", service.board(p.id(), activityId, vehicleId, r == null ? null : r.userId()));
    }
    @DeleteMapping("/{vehicleId}/passengers/{userId}") ApiResponse<VehicleService.VehicleView> alight(@AuthenticationPrincipal UserPrincipal p,
            @PathVariable Long activityId, @PathVariable Long vehicleId, @PathVariable Long userId) {
        return ApiResponse.ok("已取消座位", service.alight(p.id(), activityId, vehicleId, userId));
    }

    public record Request(@NotBlank @Size(max = 30) String plateNumber, @NotBlank @Size(max = 50) String driverName,
                          @NotNull LocalDateTime pickupTime, @NotBlank @Size(max = 200) String pickupLocation,
                          @Min(value = 1, message = "座位数至少为 1") @Max(value = 80, message = "座位数不能超过 80") Integer seats) {
        VehicleService.Command command() { return new VehicleService.Command(plateNumber, driverName, pickupTime, pickupLocation, seats == null ? 4 : seats); }
    }
    public record BoardRequest(Long userId) {}
}
