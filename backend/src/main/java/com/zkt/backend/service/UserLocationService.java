package com.zkt.backend.service;

import com.zkt.backend.entity.UserLocation;
import com.zkt.backend.mapper.UserLocationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserLocationService {

    @Autowired
    private UserLocationMapper userLocationMapper;

    /**
     * 更新用户位置信息
     */
    public boolean updateLocation(UserLocation userLocation) {
        try {
            int result = userLocationMapper.insertOrUpdate(userLocation);
            return result > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取活动中所有参与者的位置信息
     */
    public List<UserLocation> getActivityLocations(Long activityId) {
        return userLocationMapper.getLocationsByActivityId(activityId);
    }

    /**
     * 获取指定用户在指定活动中的位置信息
     */
    public UserLocation getUserLocation(Long userId, Long activityId) {
        return userLocationMapper.getLocationByUserAndActivity(userId, activityId);
    }

    /**
     * 清理过期的位置信息
     */
    public void cleanExpiredLocations() {
        userLocationMapper.deleteExpiredLocations(60); // 清理60分钟前的数据
    }

    /**
     * 计算两点之间的距离（单位：米）
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // 转换为米

        return Math.round(distance * 100.0) / 100.0; // 保留两位小数
    }
}
