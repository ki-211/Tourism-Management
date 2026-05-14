package com.zkt.backend.service.impl;

import com.zkt.backend.entity.SignRecord;
import com.zkt.backend.mapper.SignRecordMapper;
import com.zkt.backend.service.SignRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class SignRecordServiceImpl implements SignRecordService {

    @Autowired
    private SignRecordMapper signRecordMapper;

    @Override
    public void doSign(Long taskId, Long userId) {
        // 先判断是否已经签到
        SignRecord existing = signRecordMapper.selectByTaskIdAndUserId(taskId, userId);
        if (existing != null) {
            throw new RuntimeException("您已经签到过了");
        }

        SignRecord record = new SignRecord();
        record.setTaskId(taskId);
        record.setUserId(userId);
        record.setSignTime(LocalDateTime.now());

        signRecordMapper.insert(record);
    }

    @Override
    public List<Map<String, Object>> listByUser(Long userId) {
        return signRecordMapper.selectByUser(userId);
    }

    @Override
    public List<Map<String, Object>> listByActivity(Long activityId) {
        return signRecordMapper.selectByActivity(activityId);
    }

    @Override
    public List<Map<String, Object>> statusByActivity(Long activityId) {
        return signRecordMapper.selectStatusByActivity(activityId);
    }

    @Override
    public List<Map<String, Object>> listByTask(Long taskId) {
        List<SignRecord> records = signRecordMapper.selectByTaskId(taskId);
        return records.stream()
            .map(r -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", r.getId());
                map.put("taskId", r.getTaskId());
                map.put("userId", r.getUserId());
                map.put("signTime", r.getSignTime());
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public Map<String, Object> getTaskDetail(Long taskId) {
        return signRecordMapper.selectTaskDetail(taskId);
    }
}

