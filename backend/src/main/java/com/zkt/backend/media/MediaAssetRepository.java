package com.zkt.backend.media;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, Long> {
    Optional<MediaAsset> findByObjectKey(String objectKey);
    @Query("select m from MediaAsset m where not exists(select a.id from Activity a where a.coverMediaId=m.id) and not exists(select p.id from ActivityPhoto p where p.mediaId=m.id) and not exists(select r.id from SignRecord r where r.photoMediaId=m.id) order by m.createdAt")
    List<MediaAsset> findOrphans(Pageable pageable);
}
