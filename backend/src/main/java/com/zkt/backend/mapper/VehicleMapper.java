package com.zkt.backend.mapper;

import com.zkt.backend.entity.Vehicle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface VehicleMapper {
    int insert(Vehicle vehicle);

    List<Vehicle> selectByActivityId(@Param("activityId") Long activityId);

    @Select("SELECT * FROM vehicle WHERE activity_id = #{activityId} ORDER BY create_time DESC")
    List<Vehicle> findByActivityId(@Param("activityId") Long activityId);
}
