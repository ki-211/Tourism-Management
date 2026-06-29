package com.zkt.backend.service;

import com.zkt.backend.entity.Activity;

import java.util.List;

public interface ActivityService {
    void create(Activity activity);
    Activity getById(Long id);
    List<Activity> getAll();
    List<Activity> getByUser(Long userId);
    List<Activity> getActivitiesByCreatorId(Long userId);
    void transferCreator(Long activityId, Long currentUserId, Long newCreatorId);
}

