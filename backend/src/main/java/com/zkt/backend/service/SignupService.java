package com.zkt.backend.service;

import com.zkt.backend.entity.Activity;
import com.zkt.backend.entity.Signup;

import java.util.List;

public interface SignupService {
    void signup(Signup signup);
    List<Signup> getSignupList(Long activityId);
    List<Activity> getUserActivities(Long userId);
    
    /**
     * 检查用户是否已报名某个活动
     * @param activityId 活动ID
     * @param userId 用户ID
     * @return true-已报名，false-未报名
     */
    boolean hasUserSignedUp(Long activityId, Long userId);
}
