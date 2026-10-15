package com.zkt.backend.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VehiclePassengerRepository extends JpaRepository<VehiclePassenger, Long> {
    List<VehiclePassenger> findByActivityIdOrderByCreatedAt(Long activityId);
    List<VehiclePassenger> findByVehicleId(Long vehicleId);
    Optional<VehiclePassenger> findByActivityIdAndUserId(Long activityId, Long userId);
    void deleteByVehicleId(Long vehicleId);
    void deleteByActivityIdAndUserId(Long activityId, Long userId);
    void deleteByUserId(Long userId);
}
