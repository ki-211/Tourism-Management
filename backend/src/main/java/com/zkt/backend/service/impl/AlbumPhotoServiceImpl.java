package com.zkt.backend.service.impl;

import com.zkt.backend.entity.AlbumPhoto;
import com.zkt.backend.entity.Signup;
import com.zkt.backend.mapper.AlbumPhotoMapper;
import com.zkt.backend.mapper.SignupMapper;
import com.zkt.backend.service.AlbumPhotoService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AlbumPhotoServiceImpl implements AlbumPhotoService {

    @Resource
    private AlbumPhotoMapper albumPhotoMapper;
    @Resource
    private SignupMapper signupMapper;

    @Override
    public void add(AlbumPhoto photo) {
        Long activityId = photo.getActivityId();
        Long userId = photo.getUserId();
        if (activityId == null || userId == null || photo.getUrl() == null || photo.getUrl().isEmpty()) {
            throw new IllegalArgumentException("参数不完整");
        }
        Signup s = signupMapper.findByActivityAndUser(activityId, userId);
        if (s == null || s.getStatus() == null || s.getStatus() != 1) {
            throw new RuntimeException("仅参加活动的用户可以上传图片");
        }
        albumPhotoMapper.insert(photo);
    }

    @Override
    public List<AlbumPhoto> listByActivity(Long activityId) {
        return albumPhotoMapper.selectByActivityId(activityId);
    }
}
