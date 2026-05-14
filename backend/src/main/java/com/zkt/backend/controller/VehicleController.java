package com.zkt.backend.controller;

import com.zkt.backend.common.Result;
import com.zkt.backend.entity.Vehicle;
import com.zkt.backend.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicle")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    /**
     * 发布车辆信息
     */
    @PostMapping("/add")
    public Result<?> addVehicle(@RequestBody Vehicle vehicle) {
        try {
            vehicleService.addVehicle(vehicle);
            return Result.success("发布成功");
        } catch (Exception e) {
            return Result.error("发布失败：" + e.getMessage());
        }
    }

    /**
     * 根据活动ID查询车辆列表
     */
    @GetMapping("/list/{activityId}")
    public Result<List<Vehicle>> listByActivity(@PathVariable Long activityId) {
        try {
            List<Vehicle> vehicles = vehicleService.getVehiclesByActivityId(activityId);
            return Result.success(vehicles);
        } catch (Exception e) {
            return Result.error("获取车辆列表失败：" + e.getMessage());
        }
    }
}
