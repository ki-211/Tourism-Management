package com.zkt.backend.media;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaMaintenanceService {
    private final MediaAssetRepository assets;
    private final MediaService media;

    public MediaMaintenanceService(MediaAssetRepository assets, MediaService media) {
        this.assets = assets; this.media = media;
    }

    @Scheduled(cron = "${app.storage.orphan-cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void removeOrphans() {
        assets.findOrphans(PageRequest.of(0, 100)).forEach(media::removeAfterCommit);
    }
}
