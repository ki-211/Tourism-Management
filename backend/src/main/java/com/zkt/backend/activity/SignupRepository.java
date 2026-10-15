package com.zkt.backend.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SignupRepository extends JpaRepository<Signup, Long> {
    boolean existsByActivityIdAndUserId(Long activityId, Long userId);
    Optional<Signup> findByActivityIdAndUserId(Long activityId, Long userId);
    List<Signup> findByActivityIdOrderByJoinedAt(Long activityId);
    void deleteByActivityIdAndUserId(Long activityId, Long userId);
    void deleteByUserId(Long userId);
    long countByActivityId(Long activityId);
    /** Rows of [activityId, memberCount]. */
    @Query("select s.activityId, count(s) from Signup s where s.activityId in :activityIds group by s.activityId")
    List<Object[]> countByActivityIds(Collection<Long> activityIds);
    @Query("select s.activityId from Signup s where s.userId = :userId and s.activityId in :activityIds")
    List<Long> findJoinedActivityIds(Long userId, Collection<Long> activityIds);
}
