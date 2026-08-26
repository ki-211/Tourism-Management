package com.zkt.backend.location;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SharedLocationRepository extends JpaRepository<SharedLocation, Long> {
    Optional<SharedLocation> findByActivityIdAndUserId(Long activityId, Long userId);
    List<SharedLocation> findByActivityIdAndExpiresAtAfterOrderByUpdatedAtDesc(Long activityId, LocalDateTime now);
    List<SharedLocation> findByExpiresAtBefore(LocalDateTime time);
    void deleteByActivityIdAndUserId(Long activityId, Long userId);
}
