package com.zkt.backend.room;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByActivityIdAndIdGreaterThanOrderByIdAsc(Long activityId, Long afterId, Pageable pageable);
}
