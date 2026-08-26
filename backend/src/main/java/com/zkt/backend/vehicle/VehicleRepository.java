package com.zkt.backend.vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface VehicleRepository extends JpaRepository<Vehicle,Long>{List<Vehicle> findByActivityIdOrderByPickupTime(Long activityId);}
