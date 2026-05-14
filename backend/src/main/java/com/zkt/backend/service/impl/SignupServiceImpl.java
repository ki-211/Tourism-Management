package com.zkt.backend.service.impl;

import com.zkt.backend.entity.Activity;
import com.zkt.backend.entity.Signup;
import com.zkt.backend.mapper.SignupMapper;
import com.zkt.backend.service.SignupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SignupServiceImpl implements SignupService {

    @Autowired
    private SignupMapper signupMapper;

    @Override
    public void signup(Signup signup) {
        //TODO 过期不能报名，团长可以修改报名时间
        Signup byActivityAndUser = signupMapper.findByActivityAndUser(signup.getActivityId(), signup.getUserId());
        if(byActivityAndUser != null){
            signup.setStatus(2);// 报名失败，已经报名了；
            return;
        }
        signup.setStatus(1); // 默认直接成功报名
        signupMapper.insert(signup);
    }

    @Override
    public List<Signup> getSignupList(Long activityId) {
        return signupMapper.findByActivityId(activityId);
    }

    @Override
    public List<Activity> getUserActivities(Long userId) {
        return signupMapper.findActivitiesByUserId(userId);
    }

    @Override
    public boolean hasUserSignedUp(Long activityId, Long userId) {
        if (activityId == null || userId == null) {
            return false;
        }
        Signup signup = signupMapper.findByActivityAndUser(activityId, userId);
        return signup != null;
    }
}
