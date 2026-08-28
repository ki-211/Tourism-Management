package com.zkt.backend.activity;

import com.zkt.backend.auth.User;
import com.zkt.backend.auth.UserRepository;
import com.zkt.backend.common.DomainException;
import com.zkt.backend.media.MediaAsset;
import com.zkt.backend.media.MediaAssetRepository;
import com.zkt.backend.media.MediaService;
import com.zkt.backend.location.SharedLocationRepository;
import com.zkt.backend.room.RoomEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityService {
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final ActivityRepository activities;
    private final SignupRepository signups;
    private final UserRepository users;
    private final MediaAssetRepository assets;
    private final MediaService media;
    private final RoomEventPublisher events;
    private final SharedLocationRepository locations;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public ActivityService(ActivityRepository activities, SignupRepository signups, UserRepository users,
                           MediaAssetRepository assets, MediaService media, RoomEventPublisher events,
                           SharedLocationRepository locations, Clock clock) {
        this.activities = activities; this.signups = signups; this.users = users; this.assets = assets; this.media = media; this.events = events;
        this.locations = locations;
        this.clock = clock;
    }

    @Transactional
    public ActivityView create(Long userId, CreateCommand c) {
        validateTimes(c.signupStart(), c.signupEnd(), c.startTime(), c.endTime());
        Activity a = new Activity(); a.setTitle(c.title().trim()); a.setDescription(c.description()); a.setLocation(c.location().trim());
        a.setStartTime(c.startTime()); a.setEndTime(c.endTime()); a.setSignupStart(c.signupStart()); a.setSignupEnd(c.signupEnd());
        a.setFeeRule(c.feeRule()); a.setVisibility(c.visibility()); a.setCreatorId(userId);
        if (c.visibility() == ActivityVisibility.INVITE_ONLY) a.setInvitationCode(uniqueCode());
        activities.save(a);
        Signup creator = new Signup(); creator.setActivityId(a.getId()); creator.setUserId(userId); creator.setRemark("活动负责人");
        signups.save(creator);
        return view(a, userId);
    }

    @Transactional
    public ActivityView join(Long userId, Long activityId, JoinCommand c) {
        Activity a = find(activityId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isBefore(a.getSignupStart()) || now.isAfter(a.getSignupEnd()))
            throw DomainException.badRequest("SIGNUP_CLOSED", "当前不在报名时间内");
        if (signups.existsByActivityIdAndUserId(activityId, userId))
            throw DomainException.conflict("ALREADY_JOINED", "已经报名该活动");
        if (a.getVisibility() == ActivityVisibility.INVITE_ONLY &&
                (c.invitationCode() == null || !a.getInvitationCode().equalsIgnoreCase(c.invitationCode().trim())))
            throw DomainException.forbidden("邀请码不正确");
        Signup s = new Signup(); s.setActivityId(activityId); s.setUserId(userId); s.setGrade(c.grade());
        s.setPassengerCount(c.passengerCount()); s.setRemark(c.remark()); signups.save(s);
        return view(a, userId);
    }

    @Transactional
    public void leave(Long userId, Long activityId) {
        Activity a = find(activityId);
        if (a.getCreatorId().equals(userId))
            throw DomainException.conflict("CREATOR_MUST_TRANSFER", "活动负责人需先转让负责人，才能退出活动");
        if (!signups.existsByActivityIdAndUserId(activityId, userId))
            throw DomainException.badRequest("NOT_MEMBER", "尚未加入该活动");
        locations.deleteByActivityIdAndUserId(activityId, userId);
        signups.deleteByActivityIdAndUserId(activityId, userId);
        MemberLeft payload = new MemberLeft(userId);
        events.publish(activityId, "LOCATION_REMOVED", payload);
        events.publish(activityId, "MEMBER_LEFT", payload);
    }

    @Transactional
    public ActivityView transfer(Long actorId, Long activityId, Long newCreatorId) {
        Activity a = creatorActivity(actorId, activityId);
        if (actorId.equals(newCreatorId)) throw DomainException.badRequest("SAME_CREATOR", "新负责人不能是自己");
        if (!signups.existsByActivityIdAndUserId(activityId, newCreatorId))
            throw DomainException.badRequest("NOT_MEMBER", "新负责人必须是活动参与者");
        a.setCreatorId(newCreatorId);
        events.publish(activityId, "CREATOR_TRANSFERRED", new CreatorTransfer(actorId, newCreatorId));
        return view(a, actorId);
    }

    @Transactional
    public ActivityView rotateInvitation(Long actorId, Long activityId) {
        Activity a = creatorActivity(actorId, activityId);
        if (a.getVisibility() != ActivityVisibility.INVITE_ONLY)
            throw DomainException.badRequest("PUBLIC_ACTIVITY", "公开活动不需要邀请码");
        a.setInvitationCode(uniqueCode()); return view(a, actorId);
    }

    @Transactional
    public ActivityView uploadCover(Long actorId, Long activityId, MultipartFile file) {
        Activity a = creatorActivity(actorId, activityId);
        MediaAsset old = a.getCoverMediaId() == null ? null : assets.findById(a.getCoverMediaId()).orElse(null);
        MediaAsset asset = media.saveImage(actorId, "COVER", file); a.setCoverMediaId(asset.getId());
        activities.saveAndFlush(a);
        if (old != null) media.removeAfterCommit(old);
        return view(a, actorId);
    }

    @Transactional(readOnly = true)
    public ActivityView detail(Long userId, Long id, String invitationCode) {
        Activity a = find(id);
        boolean validInvitation = invitationCode != null && a.getInvitationCode() != null
                && a.getInvitationCode().equalsIgnoreCase(invitationCode.trim());
        if (a.getVisibility() == ActivityVisibility.INVITE_ONLY && !isMember(id, userId) && !validInvitation)
            throw DomainException.forbidden("该活动仅受邀成员可见");
        return view(a, userId);
    }

    @Transactional(readOnly = true)
    public InvitationPreview invitation(String code) {
        Activity a = activities.findByInvitationCode(code.toUpperCase()).orElseThrow(() -> DomainException.notFound("邀请码无效"));
        return new InvitationPreview(a.getId(), a.getTitle(), a.getLocation(), a.getStartTime(), a.getEndTime(), a.getSignupEnd());
    }

    @Transactional(readOnly = true)
    public Page<ActivityView> list(Long userId, String scope, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        Page<Activity> result = switch (scope == null ? "discover" : scope) {
            case "joined" -> activities.findJoined(userId, pr);
            case "created" -> activities.findByCreatorIdOrderByCreatedAtDesc(userId, pr);
            case "open" -> activities.findOpen(ActivityVisibility.PUBLIC, LocalDateTime.now(clock), pr);
            default -> activities.findByVisibilityOrderByCreatedAtDesc(ActivityVisibility.PUBLIC, pr);
        };
        return result.map(a -> view(a, userId));
    }

    @Transactional(readOnly = true)
    public List<MemberView> members(Long actorId, Long activityId) {
        creatorActivity(actorId, activityId);
        return signups.findByActivityIdOrderByJoinedAt(activityId).stream().map(s -> {
            User u = users.findById(s.getUserId()).orElseThrow();
            return new MemberView(u.getId(), u.getUsername(), u.getNickname(), s.getGrade(), s.getPassengerCount(), s.getRemark(), s.getJoinedAt());
        }).toList();
    }

    public boolean isMember(Long activityId, Long userId) { return signups.existsByActivityIdAndUserId(activityId, userId); }
    public void requireMember(Long activityId, Long userId) { if (!isMember(activityId, userId)) throw DomainException.forbidden("请先报名该活动"); }
    public Activity find(Long id) { return activities.findById(id).orElseThrow(() -> DomainException.notFound("活动不存在")); }
    public Activity creatorActivity(Long userId, Long id) {
        Activity a = find(id); if (!a.getCreatorId().equals(userId)) throw DomainException.forbidden("只有活动负责人可以执行该操作"); return a;
    }

    private ActivityView view(Activity a, Long viewerId) {
        User creator = users.findById(a.getCreatorId()).orElseThrow();
        String cover = a.getCoverMediaId() == null ? null : assets.findById(a.getCoverMediaId()).map(asset -> media.view(asset, viewerId)).map(MediaService.MediaView::url).orElse(null);
        boolean creatorView = a.getCreatorId().equals(viewerId);
        return new ActivityView(a.getId(), a.getTitle(), a.getDescription(), a.getLocation(), a.getStartTime(), a.getEndTime(),
                a.getSignupStart(), a.getSignupEnd(), a.getVisibility(), creatorView ? a.getInvitationCode() : null,
                a.getFeeRule(), a.getCreatorId(), creator.getNickname(), cover, isMember(a.getId(), viewerId), creatorView, a.getCreatedAt());
    }

    private String uniqueCode() {
        for (int tries = 0; tries < 20; tries++) {
            StringBuilder b = new StringBuilder(10);
            for (int i = 0; i < 10; i++) b.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            if (activities.findByInvitationCode(b.toString()).isEmpty()) return b.toString();
        }
        throw new IllegalStateException("邀请码生成失败");
    }
    private void validateTimes(LocalDateTime ss, LocalDateTime se, LocalDateTime start, LocalDateTime end) {
        if (ss == null || se == null || start == null || end == null || ss.isAfter(se) || se.isAfter(start) || !start.isBefore(end))
            throw DomainException.badRequest("INVALID_TIME_RANGE", "时间须满足：报名开始 ≤ 报名结束 ≤ 活动开始 < 活动结束");
    }

    public record CreateCommand(String title, String description, String location, LocalDateTime startTime, LocalDateTime endTime,
                                LocalDateTime signupStart, LocalDateTime signupEnd, ActivityVisibility visibility, String feeRule) {}
    public record JoinCommand(String invitationCode, String grade, Integer passengerCount, String remark) {}
    public record ActivityView(Long id, String title, String description, String location, LocalDateTime startTime, LocalDateTime endTime,
                               LocalDateTime signupStart, LocalDateTime signupEnd, ActivityVisibility visibility, String invitationCode,
                               String feeRule, Long creatorId, String creatorName, String coverUrl, boolean joined, boolean creator,
                               LocalDateTime createdAt) {}
    public record InvitationPreview(Long id, String title, String location, LocalDateTime startTime, LocalDateTime endTime, LocalDateTime signupEnd) {}
    public record MemberView(Long userId, String username, String nickname, String grade, Integer passengerCount, String remark, LocalDateTime joinedAt) {}
    public record CreatorTransfer(Long previousCreatorId, Long newCreatorId) {}
    public record MemberLeft(Long userId) {}
}
