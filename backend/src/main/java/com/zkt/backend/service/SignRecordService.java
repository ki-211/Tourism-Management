package com.zkt.backend.service;

import java.util.List;
import java.util.Map;

public interface SignRecordService {
    void doSign(Long taskId, Long userId);
    List<Map<String, Object>> listByUser(Long userId);
    List<Map<String, Object>> listByActivity(Long activityId);
    List<Map<String, Object>> statusByActivity(Long activityId);
    List<Map<String, Object>> listByTask(Long taskId);
    Map<String, Object> getTaskDetail(Long taskId);
}
