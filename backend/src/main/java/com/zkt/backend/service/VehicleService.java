package com.zkt.backend.service;

import com.zkt.backend.entity.Vehicle;

import java.util.List;

public interface VehicleService {
    void addVehicle(Vehicle vehicle);
//    List<Vehicle> getVehiclesByActivityId(Long activityId);
    List<Vehicle> getVehiclesByActivityId(Long activityId);
}
