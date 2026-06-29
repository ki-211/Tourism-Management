package com.zkt.backend.service.impl;

import com.zkt.backend.entity.Activity;
import com.zkt.backend.mapper.ActivityMapper;
import com.zkt.backend.service.ActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityServiceImpl implements ActivityService {

    @Autowired
    private ActivityMapper activityMapper;

    @Override
    public void create(Activity activity) {
        activityMapper.insert(activity);
    }

    @Override
    public Activity getById(Long id) {
        return activityMapper.selectById(id);
    }

    @Override
    public List<Activity> getAll() {
        return activityMapper.selectAll();
    }

    @Override
    public List<Activity> getByUser(Long userId) {
        return activityMapper.selectByCreator(userId);
    }

    @Override
    public List<Activity> getActivitiesByCreatorId(Long userId) {
        return activityMapper.selectByCreatorId(userId);
    }

    @Override
    public void transferCreator(Long activityId, Long currentUserId, Long newCreatorId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("活动不存在");
        }
        if (!activity.getCreatorId().equals(currentUserId)) {
            throw new IllegalArgumentException("只有团长才能转让团长身份");
        }
        if (currentUserId.equals(newCreatorId)) {
            throw new IllegalArgumentException("不能将团长转让给自己");
        }
        int rows = activityMapper.updateCreatorId(activityId, newCreatorId);
        if (rows == 0) {
            throw new RuntimeException("转让失败");
        }
    }
}

