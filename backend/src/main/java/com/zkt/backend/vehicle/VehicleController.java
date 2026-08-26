package com.zkt.backend.vehicle;

import com.zkt.backend.auth.UserPrincipal;
import com.zkt.backend.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController @RequestMapping("/api/v1/activities/{activityId}/vehicles")
public class VehicleController {
    private final VehicleService service;public VehicleController(VehicleService service){this.service=service;}
    @GetMapping ApiResponse<List<VehicleService.VehicleView>> list(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long activityId){return ApiResponse.ok(service.list(p.id(),activityId));}
    @PostMapping ApiResponse<VehicleService.VehicleView> create(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long activityId,@Valid @RequestBody Request r){return ApiResponse.ok("车辆已发布",service.create(p.id(),activityId,new VehicleService.Command(r.plateNumber(),r.driverName(),r.pickupTime(),r.pickupLocation())));}
    public record Request(@NotBlank @Size(max=30) String plateNumber,@NotBlank @Size(max=50) String driverName,@NotNull LocalDateTime pickupTime,@NotBlank @Size(max=200) String pickupLocation){}
}
