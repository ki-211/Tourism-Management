package com.zkt.backend.room;

import com.zkt.backend.activity.ActivityService;
import com.zkt.backend.auth.User;
import com.zkt.backend.auth.UserRepository;
import com.zkt.backend.location.SharedLocation;
import com.zkt.backend.location.SharedLocationRepository;
import com.zkt.backend.media.MediaAsset;
import com.zkt.backend.media.MediaAssetRepository;
import com.zkt.backend.media.MediaService;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RoomService {
    private final ActivityService activities; private final UserRepository users;
    private final ChatMessageRepository messages; private final ActivityPhotoRepository photos;
    private final SharedLocationRepository locations; private final MediaService media;
    private final MediaAssetRepository assets; private final RoomEventPublisher events;
    public RoomService(ActivityService activities, UserRepository users, ChatMessageRepository messages,
                       ActivityPhotoRepository photos, SharedLocationRepository locations, MediaService media,
                       MediaAssetRepository assets, RoomEventPublisher events) {
        this.activities=activities; this.users=users; this.messages=messages; this.photos=photos; this.locations=locations;
        this.media=media; this.assets=assets; this.events=events;
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
        activities.requireMember(activityId, userId); return photos.findByActivityIdOrderByCreatedAtDesc(activityId).stream().map(this::photoView).toList();
    }
    @Transactional
    public PhotoView uploadPhoto(Long userId, Long activityId, MultipartFile file) {
        activities.requireMember(activityId, userId); MediaAsset asset = media.saveImage(userId, "ALBUM", file);
        ActivityPhoto p = new ActivityPhoto(); p.setActivityId(activityId); p.setUploaderId(userId); p.setMediaId(asset.getId());
        PhotoView view = photoView(photos.save(p)); events.publish(activityId, "PHOTO_ADDED", view); return view;
    }
    @Transactional(readOnly = true)
    public List<LocationView> locations(Long userId, Long activityId) {
        activities.requireMember(activityId, userId);
        return locations.findByActivityIdAndExpiresAtAfterOrderByUpdatedAtDesc(activityId, LocalDateTime.now()).stream().map(this::locationView).toList();
    }
    @Transactional
    public LocationView updateLocation(Long userId, Long activityId, BigDecimal lat, BigDecimal lon, String address) {
        activities.requireMember(activityId, userId);
        SharedLocation l = locations.findByActivityIdAndUserId(activityId,userId).orElseGet(SharedLocation::new);
        l.setActivityId(activityId); l.setUserId(userId); l.setLatitude(lat); l.setLongitude(lon); l.setAddress(address);
        l.setExpiresAt(LocalDateTime.now().plusSeconds(60)); LocationView view=locationView(locations.save(l));
        events.publish(activityId,"LOCATION_UPDATED",view); return view;
    }
    @Transactional public void stopLocation(Long userId, Long activityId) {
        activities.requireMember(activityId,userId); locations.deleteByActivityIdAndUserId(activityId,userId);
        events.publish(activityId,"LOCATION_REMOVED",new RemovedLocation(userId));
    }
    @Scheduled(fixedDelay = 30000) @Transactional public void expireLocations() {
        List<SharedLocation> expired=locations.findByExpiresAtBefore(LocalDateTime.now()); locations.deleteAll(expired);
        expired.forEach(l->events.publish(l.getActivityId(),"LOCATION_REMOVED",new RemovedLocation(l.getUserId())));
    }
    private MessageView messageView(ChatMessage m) { User u=users.findById(m.getUserId()).orElseThrow(); return new MessageView(m.getId(),m.getUserId(),u.getNickname(),m.getContent(),m.getCreatedAt()); }
    private PhotoView photoView(ActivityPhoto p) { User u=users.findById(p.getUploaderId()).orElseThrow(); MediaAsset a=assets.findById(p.getMediaId()).orElseThrow(); return new PhotoView(p.getId(),media.view(a).url(),p.getUploaderId(),u.getNickname(),p.getCreatedAt()); }
    private LocationView locationView(SharedLocation l) { User u=users.findById(l.getUserId()).orElseThrow(); return new LocationView(l.getUserId(),u.getNickname(),l.getLatitude(),l.getLongitude(),l.getAddress(),l.getUpdatedAt(),l.getExpiresAt()); }
    public record MessageView(Long id,Long userId,String nickname,String content,LocalDateTime createdAt){}
    public record PhotoView(Long id,String url,Long uploaderId,String uploaderName,LocalDateTime createdAt){}
    public record LocationView(Long userId,String nickname,BigDecimal latitude,BigDecimal longitude,String address,LocalDateTime updatedAt,LocalDateTime expiresAt){}
    public record RemovedLocation(Long userId){}
}
