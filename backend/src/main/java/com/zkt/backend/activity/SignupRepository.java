package com.zkt.backend.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SignupRepository extends JpaRepository<Signup, Long> {
    boolean existsByActivityIdAndUserId(Long activityId, Long userId);
    Optional<Signup> findByActivityIdAndUserId(Long activityId, Long userId);
    List<Signup> findByActivityIdOrderByJoinedAt(Long activityId);
}
