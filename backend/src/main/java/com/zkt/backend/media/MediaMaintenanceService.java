package com.zkt.backend.media;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class MediaMaintenanceService {
    static final int BATCH_SIZE = 100;
    private static final int MAX_BATCHES = 200;
    // Every asset is referenced in the transaction that creates it; the grace period is a safety margin.
    private static final long MIN_AGE_HOURS = 1;
    private final MediaAssetRepository assets;
    private final MediaService media;
    private final TransactionTemplate transactions;
    private final Clock clock;

    public MediaMaintenanceService(MediaAssetRepository assets, MediaService media, TransactionTemplate transactions, Clock clock) {
        this.assets = assets; this.media = media; this.transactions = transactions; this.clock = clock;
    }

    /** Removes unreferenced assets in batches, each in its own transaction, until none are left. */
    @Scheduled(cron = "${app.storage.orphan-cleanup-cron:0 30 3 * * *}")
    @SchedulerLock(name = "removeOrphanMedia", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public void removeOrphans() {
        LocalDateTime createdBefore = LocalDateTime.now(clock).minusHours(MIN_AGE_HOURS);
        int total = 0;
        for (int batch = 0; batch < MAX_BATCHES; batch++) {
            Integer removed = transactions.execute(status -> {
                List<MediaAsset> orphans = assets.findOrphans(createdBefore, PageRequest.of(0, BATCH_SIZE));
                orphans.forEach(media::removeAfterCommit);
                return orphans.size();
            });
            total += removed == null ? 0 : removed;
            if (removed == null || removed < BATCH_SIZE) break;
        }
        if (total > 0) log.info("Removed orphan media count={}", total);
    }
}
