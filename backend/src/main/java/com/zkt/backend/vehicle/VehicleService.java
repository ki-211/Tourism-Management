package com.zkt.backend.vehicle;

import com.zkt.backend.activity.ActivityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VehicleService {
    private final VehicleRepository vehicles; private final ActivityService activities;
    public VehicleService(VehicleRepository vehicles,ActivityService activities){this.vehicles=vehicles;this.activities=activities;}
    @Transactional(readOnly=true) public List<VehicleView> list(Long userId,Long activityId){activities.requireMember(activityId,userId);return vehicles.findByActivityIdOrderByPickupTime(activityId).stream().map(VehicleView::from).toList();}
    @Transactional public VehicleView create(Long userId,Long activityId,Command c){activities.creatorActivity(userId,activityId);Vehicle v=new Vehicle();v.setActivityId(activityId);v.setPlateNumber(c.plateNumber().trim().toUpperCase());v.setDriverName(c.driverName().trim());v.setPickupTime(c.pickupTime());v.setPickupLocation(c.pickupLocation().trim());v.setCreatedBy(userId);return VehicleView.from(vehicles.save(v));}
    public record Command(String plateNumber,String driverName,LocalDateTime pickupTime,String pickupLocation){}
    public record VehicleView(Long id,String plateNumber,String driverName,LocalDateTime pickupTime,String pickupLocation,LocalDateTime createdAt){static VehicleView from(Vehicle v){return new VehicleView(v.getId(),v.getPlateNumber(),v.getDriverName(),v.getPickupTime(),v.getPickupLocation(),v.getCreatedAt());}}
}
