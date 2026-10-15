package com.zkt.backend.attendance;

import com.zkt.backend.activity.*;
import com.zkt.backend.auth.User;
import com.zkt.backend.auth.UserRepository;
import com.zkt.backend.common.DomainException;
import com.zkt.backend.common.Geo;
import com.zkt.backend.media.*;
import com.zkt.backend.room.RoomEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AttendanceService {
    private final SignTaskRepository tasks;
    private final SignRecordRepository records;
    private final ActivityService activities;
    private final ActivityRepository activityRepository;
    private final SignupRepository signups;
    private final UserRepository users;
    private final MediaService media;
    private final MediaAssetRepository assets;
    private final RoomEventPublisher events;
    private final Clock clock;

    public AttendanceService(SignTaskRepository tasks, SignRecordRepository records, ActivityService activities,
                             ActivityRepository activityRepository, SignupRepository signups, UserRepository users,
                             MediaService media, MediaAssetRepository assets, RoomEventPublisher events, Clock clock) {
        this.tasks = tasks; this.records = records; this.activities = activities; this.activityRepository = activityRepository;
        this.signups = signups; this.users = users; this.media = media; this.assets = assets; this.events = events; this.clock = clock;
    }

    @Transactional
    public TaskView createTask(Long userId, Long activityId, TaskCommand c) {
        activities.requireActive(activities.creatorActivity(userId, activityId));
        if (c.deadline() != null && !c.deadline().isAfter(now()))
            throw DomainException.badRequest("INVALID_DEADLINE", "签到截止时间必须晚于当前时间");
        boolean anyGeo = c.latitude() != null || c.longitude() != null || c.radiusMeters() != null;
        boolean fullGeo = c.latitude() != null && c.longitude() != null && c.radiusMeters() != null;
        if (anyGeo && !fullGeo) throw DomainException.badRequest("INVALID_GEOFENCE", "签到范围需要同时提供签到点坐标和半径");
        SignTask t = new SignTask();
        t.setActivityId(activityId); t.setTitle(c.title().trim()); t.setDescription(c.description()); t.setCreatedBy(userId);
        t.setDeadline(c.deadline()); t.setPhotoRequired(c.photoRequired());
        if (fullGeo) {
            t.setLatitude(c.latitude()); t.setLongitude(c.longitude()); t.setRadiusMeters(c.radiusMeters());
            t.setLocationName(c.locationName() == null || c.locationName().isBlank() ? null : c.locationName().trim());
        }
        TaskView v = taskViews(List.of(tasks.save(t)), userId).get(0);
        events.publish(activityId, "SIGN_TASK_CREATED", v);
        return v;
    }

    @Transactional(readOnly = true)
    public List<TaskView> listTasks(Long userId, Long activityId) {
        activities.requireMember(activityId, userId);
        return taskViews(tasks.findByActivityIdOrderByCreatedAtDesc(activityId), userId);
    }

    @Transactional(readOnly = true)
    public List<TaskView> unsigned(Long userId) { return taskViews(tasks.findUnsigned(userId, now()), userId); }

    @Transactional(readOnly = true)
    public TaskView task(Long userId, Long taskId) {
        SignTask t = findTask(taskId);
        activities.requireMember(t.getActivityId(), userId);
        return taskViews(List.of(t), userId).get(0);
    }

    @Transactional
    public RecordView sign(Long userId, Long taskId, BigDecimal lat, BigDecimal lon, String address, String remark, MultipartFile photo) {
        SignTask t = findTask(taskId);
        activities.requireMember(t.getActivityId(), userId);
        activities.activeActivity(t.getActivityId());
        if (records.existsByTaskIdAndUserId(taskId, userId)) throw DomainException.conflict("ALREADY_SIGNED", "已经签到过了");
        if (t.getDeadline() != null && now().isAfter(t.getDeadline())) throw DomainException.conflict("SIGN_CLOSED", "签到已截止");
        if (t.isPhotoRequired() && (photo == null || photo.isEmpty())) throw DomainException.badRequest("PHOTO_REQUIRED", "该签到需要上传现场照片");
        Integer distance = null;
        if (t.getRadiusMeters() != null) {
            // Coordinates come from the client, so this deters casual misuse rather than determined spoofing.
            if (lat == null || lon == null) throw DomainException.badRequest("LOCATION_REQUIRED", "该签到需要定位，请开启定位后重试");
            distance = (int) Math.round(Geo.distanceMeters(t.getLatitude(), t.getLongitude(), lat, lon));
            if (distance > t.getRadiusMeters())
                throw DomainException.badRequest("OUT_OF_RANGE", "距签到点约 " + distance + " 米，需在 " + t.getRadiusMeters() + " 米范围内签到");
        }
        SignRecord r = new SignRecord();
        r.setTaskId(taskId); r.setUserId(userId); r.setLatitude(lat); r.setLongitude(lon); r.setAddress(address); r.setRemark(remark);
        r.setDistanceMeters(distance);
        if (photo != null && !photo.isEmpty()) r.setPhotoMediaId(media.saveImage(userId, "SIGN", photo).getId());
        RecordView v = recordViews(List.of(records.save(r)), userId).get(0);
        events.publish(t.getActivityId(), "SIGN_RECORD_CREATED", new SignRecordCreated(r.getId(), r.getTaskId(), r.getUserId()));
        return v;
    }

    @Transactional(readOnly = true)
    public TaskSummary summary(Long userId, Long taskId) {
        SignTask t = findTask(taskId);
        activities.creatorActivity(userId, t.getActivityId());
        Map<Long, RecordView> signed = recordViews(records.findByTaskIdOrderBySignedAt(taskId), userId).stream()
                .collect(Collectors.toMap(RecordView::userId, Function.identity()));
        List<Signup> members = signups.findByActivityIdOrderByJoinedAt(t.getActivityId());
        Map<Long, User> byId = usersById(members.stream().map(Signup::getUserId).toList());
        List<MemberStatus> statuses = members.stream().map(s -> {
            RecordView r = signed.get(s.getUserId());
            return new MemberStatus(s.getUserId(), byId.get(s.getUserId()).getNickname(), r != null, r);
        }).toList();
        return new TaskSummary(taskViews(List.of(t), userId).get(0), statuses);
    }

    @Transactional(readOnly = true)
    public List<HistoryView> history(Long userId) {
        List<SignRecord> mine = records.findByUserIdOrderBySignedAtDesc(userId);
        if (mine.isEmpty()) return List.of();
        Map<Long, SignTask> tasksById = tasks.findAllById(mine.stream().map(SignRecord::getTaskId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(SignTask::getId, Function.identity()));
        Map<Long, Activity> activitiesById = activityRepository.findAllById(tasksById.values().stream().map(SignTask::getActivityId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Activity::getId, Function.identity()));
        return mine.stream().map(r -> {
            SignTask t = tasksById.get(r.getTaskId());
            Activity a = activitiesById.get(t.getActivityId());
            return new HistoryView(r.getId(), t.getId(), t.getTitle(), a.getId(), a.getTitle(), r.getSignedAt());
        }).toList();
    }

    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private SignTask findTask(Long id) { return tasks.findById(id).orElseThrow(() -> DomainException.notFound("签到任务不存在")); }

    // View builders issue one aggregate/IN query per list instead of per-row count/exists/findById lookups.
    private List<TaskView> taskViews(List<SignTask> list, Long userId) {
        if (list.isEmpty()) return List.of();
        List<Long> ids = list.stream().map(SignTask::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        records.countByTaskIds(ids).forEach(row -> counts.put((Long) row[0], ((Number) row[1]).longValue()));
        Set<Long> signed = new HashSet<>(records.findSignedTaskIds(userId, ids));
        LocalDateTime now = now();
        return list.stream().map(t -> new TaskView(t.getId(), t.getActivityId(), t.getTitle(), t.getDescription(), t.getCreatedAt(),
                counts.getOrDefault(t.getId(), 0L), signed.contains(t.getId()), t.getDeadline(),
                t.getDeadline() != null && now.isAfter(t.getDeadline()), t.getLatitude(), t.getLongitude(), t.getRadiusMeters(),
                t.getLocationName(), t.isPhotoRequired())).toList();
    }

    private List<RecordView> recordViews(List<SignRecord> list, Long viewerId) {
        if (list.isEmpty()) return List.of();
        Map<Long, User> byId = usersById(list.stream().map(SignRecord::getUserId).toList());
        List<Long> photoIds = list.stream().map(SignRecord::getPhotoMediaId).filter(Objects::nonNull).toList();
        Map<Long, MediaAsset> photos = photoIds.isEmpty() ? Map.of()
                : assets.findAllById(photoIds).stream().collect(Collectors.toMap(MediaAsset::getId, Function.identity()));
        return list.stream().map(r -> {
            MediaAsset asset = r.getPhotoMediaId() == null ? null : photos.get(r.getPhotoMediaId());
            MediaService.MediaView photo = asset == null ? null : media.view(asset, viewerId);
            return new RecordView(r.getId(), r.getTaskId(), r.getUserId(), byId.get(r.getUserId()).getNickname(), r.getLatitude(), r.getLongitude(),
                    r.getAddress(), r.getRemark(), photo == null ? null : photo.url(), photo == null ? null : photo.thumbnailUrl(),
                    r.getDistanceMeters(), r.getSignedAt());
        }).toList();
    }

    private Map<Long, User> usersById(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return users.findAllById(new HashSet<>(ids)).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }

    public record TaskCommand(String title, String description, LocalDateTime deadline, BigDecimal latitude, BigDecimal longitude,
                              Integer radiusMeters, String locationName, boolean photoRequired) {}
    public record TaskView(Long id, Long activityId, String title, String description, LocalDateTime createdAt, long signedCount, boolean signed,
                           LocalDateTime deadline, boolean closed, BigDecimal latitude, BigDecimal longitude, Integer radiusMeters,
                           String locationName, boolean photoRequired) {}
    public record RecordView(Long id, Long taskId, Long userId, String nickname, BigDecimal latitude, BigDecimal longitude, String address,
                             String remark, String photoUrl, String photoThumbnailUrl, Integer distanceMeters, LocalDateTime signedAt) {}
    public record MemberStatus(Long userId, String nickname, boolean signed, RecordView record) {}
    public record TaskSummary(TaskView task, List<MemberStatus> members) {}
    public record SignRecordCreated(Long recordId, Long taskId, Long userId) {}
    public record HistoryView(Long recordId, Long taskId, String taskTitle, Long activityId, String activityTitle, LocalDateTime signedAt) {}
}
