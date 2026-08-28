package com.zkt.backend.media;

import com.zkt.backend.activity.Activity;
import com.zkt.backend.activity.ActivityRepository;
import com.zkt.backend.activity.SignupRepository;
import com.zkt.backend.attendance.SignRecord;
import com.zkt.backend.attendance.SignRecordRepository;
import com.zkt.backend.attendance.SignTask;
import com.zkt.backend.attendance.SignTaskRepository;
import com.zkt.backend.common.DomainException;
import com.zkt.backend.auth.User;
import com.zkt.backend.auth.UserRepository;
import com.zkt.backend.room.ActivityPhoto;
import com.zkt.backend.room.ActivityPhotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaAuthorizationService {
    private final MediaAssetRepository assets;
    private final ActivityPhotoRepository photos;
    private final SignRecordRepository records;
    private final SignTaskRepository tasks;
    private final ActivityRepository activities;
    private final SignupRepository signups;
    private final UserRepository users;

    public MediaAuthorizationService(MediaAssetRepository assets, ActivityPhotoRepository photos,
            SignRecordRepository records, SignTaskRepository tasks, ActivityRepository activities, SignupRepository signups,
            UserRepository users) {
        this.assets = assets; this.photos = photos; this.records = records; this.tasks = tasks;
        this.activities = activities; this.signups = signups;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public void requireAccess(Long userId, Long mediaId) {
        User user = users.findById(userId).orElseThrow(() -> DomainException.forbidden("图片访问凭证无效"));
        if (user.getDeletedAt() != null) throw DomainException.forbidden("图片访问凭证已失效");
        MediaAsset asset = assets.findById(mediaId).orElseThrow(() -> DomainException.notFound("图片不存在"));
        if ("COVER".equals(asset.getPurpose())) return;
        if ("ALBUM".equals(asset.getPurpose())) {
            ActivityPhoto photo = photos.findByMediaId(mediaId).orElseThrow(() -> DomainException.notFound("图片不存在"));
            requireMember(photo.getActivityId(), userId);
            return;
        }
        if ("SIGN".equals(asset.getPurpose())) {
            SignRecord record = records.findByPhotoMediaId(mediaId).orElseThrow(() -> DomainException.notFound("图片不存在"));
            SignTask task = tasks.findById(record.getTaskId()).orElseThrow(() -> DomainException.notFound("签到任务不存在"));
            Activity activity = activities.findById(task.getActivityId()).orElseThrow(() -> DomainException.notFound("活动不存在"));
            if (!record.getUserId().equals(userId) && !activity.getCreatorId().equals(userId))
                throw DomainException.forbidden("无权查看签到证据");
            return;
        }
        throw DomainException.forbidden("无权查看图片");
    }

    private void requireMember(Long activityId, Long userId) {
        if (!signups.existsByActivityIdAndUserId(activityId, userId)) throw DomainException.forbidden("请先报名该活动");
    }
}
