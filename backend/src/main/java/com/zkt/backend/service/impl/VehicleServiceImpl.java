package com.zkt.backend.service.impl;

import com.zkt.backend.entity.Vehicle;
import com.zkt.backend.mapper.VehicleMapper;
import com.zkt.backend.service.VehicleService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class VehicleServiceImpl implements VehicleService {

    @Resource
    private VehicleMapper vehicleMapper;

    @Override
    public void addVehicle(Vehicle vehicle) {
        vehicleMapper.insert(vehicle);
    }

    @Override
    public List<Vehicle> getVehiclesByActivityId(Long activityId) {
        return vehicleMapper.findByActivityId(activityId);
    }
}
