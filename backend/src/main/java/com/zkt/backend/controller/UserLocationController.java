package com.zkt.backend.controller;

import com.zkt.backend.entity.UserLocation;
import com.zkt.backend.service.UserLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/location")
public class UserLocationController {

    @Autowired
    private UserLocationService userLocationService;

    /**
     * 更新用户位置信息
     */
    @PostMapping("/update")
    public Map<String, Object> updateLocation(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 手动从Map中提取并转换字段，兼容前端传数字或字符串
            Long userId = body.get("userId") != null ? Long.valueOf(body.get("userId").toString()) : null;
            Long activityId = body.get("activityId") != null ? Long.valueOf(body.get("activityId").toString()) : null;
            Double latitude = body.get("latitude") != null ? Double.valueOf(body.get("latitude").toString()) : null;
            Double longitude = body.get("longitude") != null ? Double.valueOf(body.get("longitude").toString()) : null;
            String address = body.get("address") != null ? body.get("address").toString() : null;

            if (userId == null || activityId == null) {
                result.put("success", false);
                result.put("message", "用户ID和活动ID不能为空");
                return result;
            }

            if (latitude == null || longitude == null) {
                result.put("success", false);
                result.put("message", "经纬度信息不能为空");
                return result;
            }

            UserLocation userLocation = new UserLocation();
            userLocation.setUserId(userId);
            userLocation.setActivityId(activityId);
            userLocation.setLatitude(latitude);
            userLocation.setLongitude(longitude);
            userLocation.setAddress(address);

            boolean success = userLocationService.updateLocation(userLocation);
            if (success) {
                result.put("success", true);
                result.put("message", "位置更新成功");
            } else {
                result.put("success", false);
                result.put("message", "位置更新失败");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "系统错误：" + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 获取活动中所有参与者的位置信息
     */
    @GetMapping("/list")
    public Map<String, Object> getActivityLocations(@RequestParam Long activityId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (activityId == null) {
                result.put("success", false);
                result.put("message", "活动ID不能为空");
                return result;
            }

            List<UserLocation> locations = userLocationService.getActivityLocations(activityId);
            result.put("success", true);
            result.put("data", locations);
            result.put("message", "获取位置信息成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "系统错误：" + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 获取指定用户的位置信息
     */
    @GetMapping("/user")
    public Map<String, Object> getUserLocation(@RequestParam Long userId, @RequestParam Long activityId) {
        Map<String, Object> result = new HashMap<>();

        try {
            if (userId == null || activityId == null) {
                result.put("success", false);
                result.put("message", "用户ID和活动ID不能为空");
                return result;
            }

            UserLocation location = userLocationService.getUserLocation(userId, activityId);
            result.put("success", true);
            result.put("data", location);
            result.put("message", "获取位置信息成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "系统错误：" + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 计算两点之间的距离
     */
    @PostMapping("/distance")
    public Map<String, Object> calculateDistance(@RequestBody Map<String, Double> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            Double lat1 = params.get("lat1");
            Double lon1 = params.get("lon1");
            Double lat2 = params.get("lat2");
            Double lon2 = params.get("lon2");

            if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
                result.put("success", false);
                result.put("message", "经纬度参数不完整");
                return result;
            }

            double distance = userLocationService.calculateDistance(lat1, lon1, lat2, lon2);
            result.put("success", true);
            result.put("distance", distance);
            result.put("message", "距离计算成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "系统错误：" + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }
}
