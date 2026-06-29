package com.zkt.backend.mapper;

import com.zkt.backend.entity.UserLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserLocationMapper {

    /**
     * 插入或更新用户位置信息
     */
    int insertOrUpdate(UserLocation userLocation);

    /**
     * 根据活动ID获取所有参与者的位置信息
     */
    List<UserLocation> getLocationsByActivityId(@Param("activityId") Long activityId);

    /**
     * 根据用户ID和活动ID获取位置信息
     */
    UserLocation getLocationByUserAndActivity(@Param("userId") Long userId, @Param("activityId") Long activityId);

    /**
     * 删除过期的位置信息（超过指定时间未更新）
     */
    int deleteExpiredLocations(@Param("minutes") int minutes);
}
