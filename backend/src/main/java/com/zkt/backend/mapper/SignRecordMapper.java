package com.zkt.backend.mapper;

import com.zkt.backend.entity.SignRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SignRecordMapper {
    // 插入签到记录
    int insert(SignRecord signRecord);

    // 根据 taskId 和 userId 查询签到记录，判断是否已签到
    SignRecord selectByTaskIdAndUserId(@Param("taskId") Long taskId, @Param("userId") Long userId);

    List<Map<String, Object>> selectByUser(@Param("userId") Long userId);

    List<Map<String, Object>> selectByActivity(@Param("activityId") Long activityId);

    List<Map<String, Object>> selectStatusByActivity(@Param("activityId") Long activityId);

    List<SignRecord> selectByTaskId(@Param("taskId") Long taskId);

    Map<String, Object> selectTaskDetail(@Param("taskId") Long taskId);
}
