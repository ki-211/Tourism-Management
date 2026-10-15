package com.zkt.backend.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface SignTaskRepository extends JpaRepository<SignTask,Long>{
    List<SignTask> findByActivityIdOrderByCreatedAtDesc(Long activityId);
    @Query("select t from SignTask t join Signup s on s.activityId=t.activityId join Activity a on a.id=t.activityId where s.userId=:userId and a.status=com.zkt.backend.activity.ActivityStatus.ACTIVE and not exists(select r.id from SignRecord r where r.taskId=t.id and r.userId=:userId) order by t.createdAt desc")
    List<SignTask> findUnsigned(Long userId);
}
