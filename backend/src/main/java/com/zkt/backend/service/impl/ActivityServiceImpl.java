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
}

