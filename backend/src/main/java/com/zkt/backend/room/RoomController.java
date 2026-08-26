package com.zkt.backend.room;

import com.zkt.backend.auth.UserPrincipal;
import com.zkt.backend.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/activities/{activityId}")
public class RoomController {
    private final RoomService service; public RoomController(RoomService service){this.service=service;}
    @GetMapping("/messages") ApiResponse<List<RoomService.MessageView>> messages(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long activityId,@RequestParam(defaultValue="0") long afterId,@RequestParam(defaultValue="50") int limit){return ApiResponse.ok(service.messages(p.id(),activityId,afterId,limit));}
    @PostMapping("/messages") ApiResponse<RoomService.MessageView> send(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long activityId,@Valid @RequestBody MessageRequest r){return ApiResponse.ok(service.send(p.id(),activityId,r.content()));}
    @GetMapping("/photos") ApiResponse<List<RoomService.PhotoView>> photos(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long activityId){return ApiResponse.ok(service.photos(p.id(),activityId));}
    @PostMapping(value="/photos",consumes="multipart/form-data") ApiResponse<RoomService.PhotoView> upload(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long activityId,@RequestPart MultipartFile file){return ApiResponse.ok("上传成功",service.uploadPhoto(p.id(),activityId,file));}
    @GetMapping("/locations") ApiResponse<List<RoomService.LocationView>> locations(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long activityId){return ApiResponse.ok(service.locations(p.id(),activityId));}
    @PutMapping("/locations/me") ApiResponse<RoomService.LocationView> update(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long activityId,@Valid @RequestBody LocationRequest r){return ApiResponse.ok(service.updateLocation(p.id(),activityId,r.latitude(),r.longitude(),r.address()));}
    @DeleteMapping("/locations/me") ApiResponse<Void> stop(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long activityId){service.stopLocation(p.id(),activityId);return ApiResponse.ok("位置共享已停止",null);}
    public record MessageRequest(@NotBlank @Size(max=1000) String content){}
    public record LocationRequest(@NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,@NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,@Size(max=500) String address){}
}
