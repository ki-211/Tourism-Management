package com.zkt.backend.mapper;

import com.zkt.backend.entity.Activity;
import com.zkt.backend.entity.Signup;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SignupMapper {
    @Insert("INSERT INTO signup (activity_id, user_id, status, signup_time, remark, grade, passenger_count) " +
            "VALUES (#{activityId}, #{userId}, #{status}, NOW(), #{remark}, #{grade}, #{passengerCount})")
    void insert(Signup signup);

    @Select("SELECT s.*, u.nickname " +
            "FROM signup s " +
            "LEFT JOIN user u ON s.user_id = u.id " +
            "WHERE s.activity_id = #{activityId}")
    List<Signup> findByActivityId(@Param("activityId") Long activityId);

    @Select("SELECT * FROM signup WHERE activity_id = #{activityId} AND user_id = #{userId} LIMIT 1")
    Signup findByActivityAndUser(@Param("activityId") Long activityId, @Param("userId") Long userId);

    // 根据用户 ID 查询其参与的活动列表
    List<Activity> findActivitiesByUserId(@Param("userId") Long userId);
}
