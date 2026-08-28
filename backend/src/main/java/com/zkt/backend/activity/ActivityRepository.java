package com.zkt.backend.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findByVisibilityOrderByCreatedAtDesc(ActivityVisibility visibility, Pageable pageable);
    Page<Activity> findByCreatorIdOrderByCreatedAtDesc(Long creatorId, Pageable pageable);
    Optional<Activity> findByInvitationCode(String code);
    boolean existsByCreatorId(Long creatorId);
    @Query("select a from Activity a join Signup s on s.activityId=a.id where s.userId=:userId order by s.joinedAt desc")
    Page<Activity> findJoined(Long userId, Pageable pageable);
    @Query("select a from Activity a where a.visibility=:visibility and a.signupStart<=:now and a.signupEnd>=:now order by a.signupEnd")
    Page<Activity> findOpen(ActivityVisibility visibility, LocalDateTime now, Pageable pageable);
}
