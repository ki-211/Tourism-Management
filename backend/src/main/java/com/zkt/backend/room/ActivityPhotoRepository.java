package com.zkt.backend.room;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ActivityPhotoRepository extends JpaRepository<ActivityPhoto, Long> {
    List<ActivityPhoto> findByActivityIdOrderByCreatedAtDesc(Long activityId);
    Optional<ActivityPhoto> findByMediaId(Long mediaId);
}
