package com.zkt.backend.room;

import com.zkt.backend.activity.ActivityService;
import com.zkt.backend.activity.Activity;
import com.zkt.backend.activity.ActivityStatus;
import com.zkt.backend.common.DomainException;
import com.zkt.backend.auth.User;
import com.zkt.backend.auth.UserRepository;
import com.zkt.backend.location.SharedLocation;
import com.zkt.backend.location.SharedLocationRepository;
import com.zkt.backend.media.MediaAsset;
import com.zkt.backend.media.MediaAssetRepository;
import com.zkt.backend.media.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RoomService {
    private final ActivityService activities; private final UserRepository users;
    private final ChatMessageRepository messages; private final ActivityPhotoRepository photos;
    private final SharedLocationRepository locations; private final MediaService media;
    private final MediaAssetRepository assets; private final RoomEventPublisher events;
    private final Clock clock;
    private final TransactionTemplate transactions;
    public RoomService(ActivityService activities, UserRepository users, ChatMessageRepository messages,
                       ActivityPhotoRepository photos, SharedLocationRepository locations, MediaService media,
                       MediaAssetRepository assets, RoomEventPublisher events, Clock clock,
                       TransactionTemplate transactions) {
        this.activities=activities; this.users=users; this.messages=messages; this.photos=photos; this.locations=locations;
        this.media=media; this.assets=assets; this.events=events;
        this.clock=clock;
        this.transactions=transactions;
    }
    @Transactional(readOnly = true)
    public List<MessageView> messages(Long userId, Long activityId, long afterId, Long beforeId, int limit) {
        activities.requireMember(activityId, userId);
        PageRequest page = PageRequest.of(0, Math.min(Math.max(limit,1),100));
        // afterId > 0: incremental catch-up in ascending order; otherwise the newest page before beforeId.
        List<ChatMessage> found = afterId > 0
                ? messages.findByActivityIdAndIdGreaterThanOrderByIdAsc(activityId, afterId, page)
                : messages.findByActivityIdAndIdLessThanOrderByIdDesc(activityId, beforeId == null ? Long.MAX_VALUE : beforeId, page).reversed();
        return messageViews(found);
    }
    @Transactional
    public MessageView send(Long userId, Long activityId, String content) {
        activities.requireMember(activityId, userId);
        activities.activeActivity(activityId);
        ChatMessage m = new ChatMessage(); m.setActivityId(activityId); m.setUserId(userId); m.setContent(content.trim());
        MessageView view = messageViews(List.of(messages.save(m))).get(0); events.publish(activityId, "CHAT_CREATED", view); return view;
    }
    @Transactional(readOnly = true)
    public List<PhotoView> photos(Long userId, Long activityId) {
        activities.requireMember(activityId, userId); return photoViews(photos.findByActivityIdOrderByCreatedAtDesc(activityId), userId, activities.find(activityId));
    }
    @Transactional
    public PhotoView uploadPhoto(Long userId, Long activityId, MultipartFile file) {
        activities.requireMember(activityId, userId); activities.activeActivity(activityId);
        MediaAsset asset = media.saveImage(userId, "ALBUM", file);
        ActivityPhoto p = new ActivityPhoto(); p.setActivityId(activityId); p.setUploaderId(userId); p.setMediaId(asset.getId());
        PhotoView view = photoViews(List.of(photos.save(p)), userId, activities.find(activityId)).get(0); events.publish(activityId, "PHOTO_ADDED", new PhotoAdded(view.id())); return view;
    }
    @Transactional
    public void deletePhoto(Long userId, Long activityId, Long photoId) {
        activities.requireMember(activityId, userId);
        ActivityPhoto photo = photos.findById(photoId).orElseThrow(() -> DomainException.notFound("照片不存在"));
        if (!photo.getActivityId().equals(activityId)) throw DomainException.notFound("照片不存在");
        Activity activity = activities.find(activityId);
        if (!photo.getUploaderId().equals(userId) && !activity.getCreatorId().equals(userId))
            throw DomainException.forbidden("只有上传者或活动负责人可以删除照片");
        MediaAsset asset = assets.findById(photo.getMediaId()).orElse(null);
        photos.delete(photo); photos.flush();
        if (asset != null) media.removeAfterCommit(asset);
        events.publish(activityId, "PHOTO_REMOVED", new PhotoRemoved(photoId));
    }
    @Transactional(readOnly = true)
    public List<LocationView> locations(Long userId, Long activityId) {
        activities.requireMember(activityId, userId);
        Activity activity = activities.find(activityId);
        if (!now().isBefore(activity.getEndTime()) || activity.getStatus() == ActivityStatus.CANCELLED) return List.of();
        return locationViews(locations.findByActivityIdAndExpiresAtAfterOrderByUpdatedAtDesc(activityId, now()));
    }
    @Transactional
    public LocationView updateLocation(Long userId, Long activityId, BigDecimal lat, BigDecimal lon, String address) {
        long started = System.nanoTime();
        try {
            activities.requireMember(activityId, userId);
            Activity activity = activities.activeActivity(activityId);
            if (!now().isBefore(activity.getEndTime()))
                throw DomainException.conflict("ACTIVITY_ENDED", "活动已结束，位置共享已停止");
            SharedLocation l = locations.findByActivityIdAndUserId(activityId,userId).orElseGet(SharedLocation::new);
            l.setActivityId(activityId); l.setUserId(userId); l.setLatitude(lat); l.setLongitude(lon); l.setAddress(address);
            l.setExpiresAt(now().plusSeconds(90)); LocationView view=locationViews(List.of(locations.save(l))).get(0);
            events.publish(activityId,"LOCATION_UPDATED",view);
            log.debug("Location updated activityId={} userId={} elapsedMs={}", activityId, userId, elapsedMillis(started));
            return view;
        } catch (RuntimeException e) {
            log.warn("Location update failed activityId={} userId={} errorType={} elapsedMs={}",
                    activityId, userId, e.getClass().getSimpleName(), elapsedMillis(started));
            throw e;
        }
    }
    @Transactional public void stopLocation(Long userId, Long activityId) {
        activities.requireMember(activityId,userId); locations.deleteByActivityIdAndUserId(activityId,userId);
        events.publish(activityId,"LOCATION_REMOVED",new RemovedLocation(userId));
    }
    @Scheduled(fixedDelay = 30000)
    @SchedulerLock(name = "expireSharedLocations", lockAtMostFor = "PT1M", lockAtLeastFor = "PT15S")
    public void expireLocations() {
        transactions.executeWithoutResult(status -> {
            List<SharedLocation> expired=locations.findByExpiresAtBefore(now()); locations.deleteAll(expired);
            expired.forEach(l->events.publish(l.getActivityId(),"LOCATION_REMOVED",new RemovedLocation(l.getUserId())));
        });
    }
    private long elapsedMillis(long started) { return java.time.Duration.ofNanos(System.nanoTime() - started).toMillis(); }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
    // View builders load related users/assets with one IN query per list instead of one lookup per row.
    private List<MessageView> messageViews(List<ChatMessage> list) {
        Map<Long, User> byId = usersById(list.stream().map(ChatMessage::getUserId).toList());
        return list.stream().map(m -> new MessageView(m.getId(), m.getUserId(), byId.get(m.getUserId()).getNickname(), m.getContent(), m.getCreatedAt())).toList();
    }
    private List<PhotoView> photoViews(List<ActivityPhoto> list, Long viewerId, Activity activity) {
        Map<Long, User> byId = usersById(list.stream().map(ActivityPhoto::getUploaderId).toList());
        Map<Long, MediaAsset> assetsById = list.isEmpty() ? Map.of()
                : assets.findAllById(list.stream().map(ActivityPhoto::getMediaId).toList()).stream().collect(Collectors.toMap(MediaAsset::getId, Function.identity()));
        boolean creator = activity.getCreatorId().equals(viewerId);
        return list.stream().map(p -> {
            MediaService.MediaView image = media.view(assetsById.get(p.getMediaId()), viewerId);
            return new PhotoView(p.getId(), image.url(), image.thumbnailUrl(), p.getUploaderId(),
                    byId.get(p.getUploaderId()).getNickname(), p.getCreatedAt(), creator || p.getUploaderId().equals(viewerId));
        }).toList();
    }
    private List<LocationView> locationViews(List<SharedLocation> list) {
        Map<Long, User> byId = usersById(list.stream().map(SharedLocation::getUserId).toList());
        return list.stream().map(l -> new LocationView(l.getUserId(), byId.get(l.getUserId()).getNickname(), l.getLatitude(), l.getLongitude(),
                l.getAddress(), l.getUpdatedAt(), l.getExpiresAt())).toList();
    }
    private Map<Long, User> usersById(Collection<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return users.findAllById(new HashSet<>(ids)).stream().collect(Collectors.toMap(User::getId, Function.identity()));
    }
    public record MessageView(Long id,Long userId,String nickname,String content,LocalDateTime createdAt){}
    public record PhotoView(Long id,String url,String thumbnailUrl,Long uploaderId,String uploaderName,LocalDateTime createdAt,boolean deletable){}
    public record PhotoAdded(Long photoId){}
    public record PhotoRemoved(Long photoId){}
    public record LocationView(Long userId,String nickname,BigDecimal latitude,BigDecimal longitude,String address,LocalDateTime updatedAt,LocalDateTime expiresAt){}
    public record RemovedLocation(Long userId){}
}
