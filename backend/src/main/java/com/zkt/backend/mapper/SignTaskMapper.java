package com.zkt.backend.mapper;

import com.zkt.backend.entity.SignTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SignTaskMapper {
    List<SignTask> selectUnSignedTasksByUserId(@Param("userId") Long userId);
    int insert(SignTask task);
    List<SignTask> selectByActivityId(@Param("activityId") Long activityId);
}
