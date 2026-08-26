package com.zkt.backend.attendance;

import com.zkt.backend.activity.*;
import com.zkt.backend.auth.User;
import com.zkt.backend.auth.UserRepository;
import com.zkt.backend.common.DomainException;
import com.zkt.backend.media.*;
import com.zkt.backend.room.RoomEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AttendanceService {
    private final SignTaskRepository tasks;private final SignRecordRepository records;private final ActivityService activities;
    private final SignupRepository signups;private final UserRepository users;private final MediaService media;private final MediaAssetRepository assets;private final RoomEventPublisher events;
    public AttendanceService(SignTaskRepository tasks,SignRecordRepository records,ActivityService activities,SignupRepository signups,UserRepository users,MediaService media,MediaAssetRepository assets,RoomEventPublisher events){this.tasks=tasks;this.records=records;this.activities=activities;this.signups=signups;this.users=users;this.media=media;this.assets=assets;this.events=events;}
    @Transactional public TaskView createTask(Long userId,Long activityId,String title,String description){activities.creatorActivity(userId,activityId);SignTask t=new SignTask();t.setActivityId(activityId);t.setTitle(title.trim());t.setDescription(description);t.setCreatedBy(userId);TaskView v=taskView(tasks.save(t),userId);events.publish(activityId,"SIGN_TASK_CREATED",v);return v;}
    @Transactional(readOnly=true) public List<TaskView> listTasks(Long userId,Long activityId){activities.requireMember(activityId,userId);return tasks.findByActivityIdOrderByCreatedAtDesc(activityId).stream().map(t->taskView(t,userId)).toList();}
    @Transactional(readOnly=true) public List<TaskView> unsigned(Long userId){return tasks.findUnsigned(userId).stream().map(t->taskView(t,userId)).toList();}
    @Transactional(readOnly=true) public TaskView task(Long userId,Long taskId){SignTask t=findTask(taskId);activities.requireMember(t.getActivityId(),userId);return taskView(t,userId);}
    @Transactional public RecordView sign(Long userId,Long taskId,BigDecimal lat,BigDecimal lon,String address,String remark,MultipartFile photo){SignTask t=findTask(taskId);activities.requireMember(t.getActivityId(),userId);if(records.existsByTaskIdAndUserId(taskId,userId))throw DomainException.conflict("ALREADY_SIGNED","已经签到过了");SignRecord r=new SignRecord();r.setTaskId(taskId);r.setUserId(userId);r.setLatitude(lat);r.setLongitude(lon);r.setAddress(address);r.setRemark(remark);if(photo!=null&&!photo.isEmpty())r.setPhotoMediaId(media.saveImage(userId,"SIGN",photo).getId());RecordView v=recordView(records.save(r));events.publish(t.getActivityId(),"SIGN_RECORD_CREATED",v);return v;}
    @Transactional(readOnly=true) public TaskSummary summary(Long userId,Long taskId){SignTask t=findTask(taskId);activities.creatorActivity(userId,t.getActivityId());Map<Long,SignRecord> signed=new HashMap<>();records.findByTaskIdOrderBySignedAt(taskId).forEach(r->signed.put(r.getUserId(),r));List<MemberStatus> members=signups.findByActivityIdOrderByJoinedAt(t.getActivityId()).stream().map(s->{User u=users.findById(s.getUserId()).orElseThrow();SignRecord r=signed.get(s.getUserId());return new MemberStatus(u.getId(),u.getNickname(),r!=null,r==null?null:recordView(r));}).toList();return new TaskSummary(taskView(t,userId),members);}
    @Transactional(readOnly=true) public List<HistoryView> history(Long userId){return records.findByUserIdOrderBySignedAtDesc(userId).stream().map(r->{SignTask t=findTask(r.getTaskId());Activity a=activities.find(t.getActivityId());return new HistoryView(r.getId(),t.getId(),t.getTitle(),a.getId(),a.getTitle(),r.getSignedAt());}).toList();}
    private SignTask findTask(Long id){return tasks.findById(id).orElseThrow(()->DomainException.notFound("签到任务不存在"));}
    private TaskView taskView(SignTask t,Long userId){return new TaskView(t.getId(),t.getActivityId(),t.getTitle(),t.getDescription(),t.getCreatedAt(),records.countByTaskId(t.getId()),records.existsByTaskIdAndUserId(t.getId(),userId));}
    private RecordView recordView(SignRecord r){User u=users.findById(r.getUserId()).orElseThrow();String photo=r.getPhotoMediaId()==null?null:assets.findById(r.getPhotoMediaId()).map(media::view).map(MediaService.MediaView::url).orElse(null);return new RecordView(r.getId(),r.getTaskId(),r.getUserId(),u.getNickname(),r.getLatitude(),r.getLongitude(),r.getAddress(),r.getRemark(),photo,r.getSignedAt());}
    public record TaskView(Long id,Long activityId,String title,String description,LocalDateTime createdAt,long signedCount,boolean signed){}
    public record RecordView(Long id,Long taskId,Long userId,String nickname,BigDecimal latitude,BigDecimal longitude,String address,String remark,String photoUrl,LocalDateTime signedAt){}
    public record MemberStatus(Long userId,String nickname,boolean signed,RecordView record){}
    public record TaskSummary(TaskView task,List<MemberStatus> members){}
    public record HistoryView(Long recordId,Long taskId,String taskTitle,Long activityId,String activityTitle,LocalDateTime signedAt){}
}
