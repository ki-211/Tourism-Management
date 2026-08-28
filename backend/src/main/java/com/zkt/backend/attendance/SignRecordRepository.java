package com.zkt.backend.attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SignRecordRepository extends JpaRepository<SignRecord,Long>{boolean existsByTaskIdAndUserId(Long taskId,Long userId);Optional<SignRecord> findByTaskIdAndUserId(Long taskId,Long userId);Optional<SignRecord> findByPhotoMediaId(Long mediaId);List<SignRecord> findByTaskIdOrderBySignedAt(Long taskId);List<SignRecord> findByUserIdOrderBySignedAtDesc(Long userId);long countByTaskId(Long taskId);}
