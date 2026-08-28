package com.zkt.backend.room;

import com.zkt.backend.activity.ActivityService;
import com.zkt.backend.activity.Activity;
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
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class RoomService {
    private final ActivityService activities; private final UserRepository users;
    private final ChatMessageRepository messages; private final ActivityPhotoRepository photos;
    private final SharedLocationRepository locations; private final MediaService media;
    private final MediaAssetRepository assets; private final RoomEventPublisher events;
    private final Clock clock;
    public RoomService(ActivityService activities, UserRepository users, ChatMessageRepository messages,
                       ActivityPhotoRepository photos, SharedLocationRepository locations, MediaService media,
                       MediaAssetRepository assets, RoomEventPublisher events, Clock clock) {
        this.activities=activities; this.users=users; this.messages=messages; this.photos=photos; this.locations=locations;
        this.media=media; this.assets=assets; this.events=events;
        this.clock=clock;
    }
    @Transactional(readOnly = true)
    public List<MessageView> messages(Long userId, Long activityId, long afterId, int limit) {
        activities.requireMember(activityId, userId);
        return messages.findByActivityIdAndIdGreaterThanOrderByIdAsc(activityId, Math.max(0, afterId), PageRequest.of(0, Math.min(Math.max(limit,1),100)))
                .stream().map(this::messageView).toList();
    }
    @Transactional
    public MessageView send(Long userId, Long activityId, String content) {
        activities.requireMember(activityId, userId);
        ChatMessage m = new ChatMessage(); m.setActivityId(activityId); m.setUserId(userId); m.setContent(content.trim());
        MessageView view = messageView(messages.save(m)); events.publish(activityId, "CHAT_CREATED", view); return view;
    }
    @Transactional(readOnly = true)
    public List<PhotoView> photos(Long userId, Long activityId) {
        activities.requireMember(activityId, userId); return photos.findByActivityIdOrderByCreatedAtDesc(activityId).stream().map(photo -> photoView(photo, userId)).toList();
    }
    @Transactional
    public PhotoView uploadPhoto(Long userId, Long activityId, MultipartFile file) {
        activities.requireMember(activityId, userId); MediaAsset asset = media.saveImage(userId, "ALBUM", file);
        ActivityPhoto p = new ActivityPhoto(); p.setActivityId(activityId); p.setUploaderId(userId); p.setMediaId(asset.getId());
        PhotoView view = photoView(photos.save(p), userId); events.publish(activityId, "PHOTO_ADDED", new PhotoAdded(view.id())); return view;
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
        if (!now().isBefore(activity.getEndTime())) return List.of();
        return locations.findByActivityIdAndExpiresAtAfterOrderByUpdatedAtDesc(activityId, now()).stream().map(this::locationView).toList();
    }
    @Transactional
    public LocationView updateLocation(Long userId, Long activityId, BigDecimal lat, BigDecimal lon, String address) {
        long started = System.nanoTime();
        try {
            activities.requireMember(activityId, userId);
            Activity activity = activities.find(activityId);
            if (!now().isBefore(activity.getEndTime()))
                throw DomainException.conflict("ACTIVITY_ENDED", "活动已结束，位置共享已停止");
            SharedLocation l = locations.findByActivityIdAndUserId(activityId,userId).orElseGet(SharedLocation::new);
            l.setActivityId(activityId); l.setUserId(userId); l.setLatitude(lat); l.setLongitude(lon); l.setAddress(address);
            l.setExpiresAt(now().plusSeconds(90)); LocationView view=locationView(locations.save(l));
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
    @Scheduled(fixedDelay = 30000) @Transactional public void expireLocations() {
        List<SharedLocation> expired=locations.findByExpiresAtBefore(now()); locations.deleteAll(expired);
        expired.forEach(l->events.publish(l.getActivityId(),"LOCATION_REMOVED",new RemovedLocation(l.getUserId())));
    }
    private long elapsedMillis(long started) { return java.time.Duration.ofNanos(System.nanoTime() - started).toMillis(); }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private MessageView messageView(ChatMessage m) { User u=users.findById(m.getUserId()).orElseThrow(); return new MessageView(m.getId(),m.getUserId(),u.getNickname(),m.getContent(),m.getCreatedAt()); }
    private PhotoView photoView(ActivityPhoto p, Long viewerId) { User u=users.findById(p.getUploaderId()).orElseThrow(); MediaAsset a=assets.findById(p.getMediaId()).orElseThrow(); Activity activity=activities.find(p.getActivityId()); boolean deletable=p.getUploaderId().equals(viewerId)||activity.getCreatorId().equals(viewerId); return new PhotoView(p.getId(),media.view(a,viewerId).url(),p.getUploaderId(),u.getNickname(),p.getCreatedAt(),deletable); }
    private LocationView locationView(SharedLocation l) { User u=users.findById(l.getUserId()).orElseThrow(); return new LocationView(l.getUserId(),u.getNickname(),l.getLatitude(),l.getLongitude(),l.getAddress(),l.getUpdatedAt(),l.getExpiresAt()); }
    public record MessageView(Long id,Long userId,String nickname,String content,LocalDateTime createdAt){}
    public record PhotoView(Long id,String url,Long uploaderId,String uploaderName,LocalDateTime createdAt,boolean deletable){}
    public record PhotoAdded(Long photoId){}
    public record PhotoRemoved(Long photoId){}
    public record LocationView(Long userId,String nickname,BigDecimal latitude,BigDecimal longitude,String address,LocalDateTime updatedAt,LocalDateTime expiresAt){}
    public record RemovedLocation(Long userId){}
}
