package com.zkt.backend.service.impl;

import com.zkt.backend.entity.SignTask;
import com.zkt.backend.mapper.SignTaskMapper;
import com.zkt.backend.service.SignTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SignTaskServiceImpl implements SignTaskService {

    @Autowired
    private SignTaskMapper signTaskMapper;

    @Override
    public List<SignTask> getUnSignedTasksByUser(Long userId) {
        return signTaskMapper.selectUnSignedTasksByUserId(userId);
    }

    @Override
    public void create(SignTask task) {
        signTaskMapper.insert(task);
    }

    @Override
    public List<SignTask> getByActivity(Long activityId) {
        return signTaskMapper.selectByActivityId(activityId);
    }
}
