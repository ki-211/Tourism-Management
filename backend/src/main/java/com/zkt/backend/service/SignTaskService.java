package com.zkt.backend.service;

import com.zkt.backend.entity.SignTask;

import java.util.List;

public interface SignTaskService {
    List<SignTask> getUnSignedTasksByUser(Long userId);
    void create(SignTask task);
    List<SignTask> getByActivity(Long activityId);
}
