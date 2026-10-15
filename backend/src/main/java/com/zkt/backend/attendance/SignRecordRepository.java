package com.zkt.backend.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SignRecordRepository extends JpaRepository<SignRecord, Long> {
    boolean existsByTaskIdAndUserId(Long taskId, Long userId);
    Optional<SignRecord> findByTaskIdAndUserId(Long taskId, Long userId);
    Optional<SignRecord> findByPhotoMediaId(Long mediaId);
    List<SignRecord> findByTaskIdOrderBySignedAt(Long taskId);
    List<SignRecord> findByUserIdOrderBySignedAtDesc(Long userId);
    long countByTaskId(Long taskId);

    /** Rows of [taskId, signedCount] for the given tasks; tasks without records are absent. */
    @Query("select r.taskId, count(r) from SignRecord r where r.taskId in :taskIds group by r.taskId")
    List<Object[]> countByTaskIds(Collection<Long> taskIds);

    @Query("select r.taskId from SignRecord r where r.userId = :userId and r.taskId in :taskIds")
    List<Long> findSignedTaskIds(Long userId, Collection<Long> taskIds);
}
