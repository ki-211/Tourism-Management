package com.zkt.backend.mapper;

import com.zkt.backend.entity.Activity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ActivityMapper {
    int insert(Activity activity);
    Activity selectById(Long id);
    List<Activity> selectAll();
    List<Activity> selectByCreator(Long userId);
    List<Activity> selectByCreatorId(@Param("creatorId") Long creatorId);
    int updateCreatorId(@Param("activityId") Long activityId, @Param("newCreatorId") Long newCreatorId);
}

