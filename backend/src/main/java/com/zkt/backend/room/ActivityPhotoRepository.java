package com.zkt.backend.room;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityPhotoRepository extends JpaRepository<ActivityPhoto, Long> {
    List<ActivityPhoto> findByActivityIdOrderByCreatedAtDesc(Long activityId);
}
