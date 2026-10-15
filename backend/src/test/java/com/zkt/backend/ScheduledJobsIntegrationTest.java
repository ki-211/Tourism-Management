package com.zkt.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkt.backend.auth.UserRepository;
import com.zkt.backend.media.MediaAssetRepository;
import com.zkt.backend.media.MediaMaintenanceService;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ScheduledJobsIntegrationTest {
    @Autowired LockProvider locks;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired MediaAssetRepository assets;
    @Autowired MediaMaintenanceService maintenance;

    @Test void aJobLockCanBeHeldByOnlyOneInstanceAtATime() {
        LockConfiguration config = new LockConfiguration(Instant.now(), "test-exclusive-job", Duration.ofMinutes(1), Duration.ZERO);
        Optional<SimpleLock> first = locks.lock(config);
        assertThat(first).isPresent();
        assertThat(locks.lock(config)).as("second instance must skip the run").isEmpty();
        first.get().unlock();
        locks.lock(config).ifPresent(SimpleLock::unlock);
    }

    @Test void orphanCleanupDrainsAllBatchesButKeepsRecentAndReferencedMedia() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", "orphan_owner", "password", "Password123!")))).andExpect(status().isOk());
        String token = mapper.readTree(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", "orphan_owner", "password", "Password123!"))))
                .andReturn().getResponse().getContentAsByteArray()).path("data").path("accessToken").asText();
        long ownerId = users.findByUsername("orphan_owner").orElseThrow().getId();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        long activityId = mapper.readTree(mvc.perform(post("/api/v1/activities").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(
                        "title", "孤儿清理", "location", "测试场地", "signupStart", now.minusMinutes(1).toString(),
                        "signupEnd", now.plusDays(1).toString(), "startTime", now.plusDays(2).toString(),
                        "endTime", now.plusDays(3).toString(), "visibility", "PUBLIC"))))
                .andReturn().getResponse().getContentAsByteArray()).path("data").path("id").asLong();
        mvc.perform(multipart("/api/v1/activities/{id}/cover", activityId)
                .file(new MockMultipartFile("file", "c.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1}))
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // LocalDateTime (not java.sql.Timestamp) is stored verbatim, matching how Hibernate writes created_at.
        LocalDateTime old = LocalDateTime.now().minusHours(2);
        int orphanCount = 250;
        jdbc.batchUpdate("insert into media_assets(owner_id, object_key, original_name, content_type, size_bytes, purpose, created_at) values (?,?,?,?,?,?,?)",
                IntStream.range(0, orphanCount).mapToObj(i -> new Object[]{ownerId, "orphan/old-" + i, "o.png", "image/png", 1, "ALBUM", old}).toList());
        jdbc.update("insert into media_assets(owner_id, object_key, original_name, content_type, size_bytes, purpose, created_at) values (?,?,?,?,?,?,?)",
                ownerId, "orphan/recent", "o.png", "image/png", 1, "ALBUM", LocalDateTime.now());
        jdbc.update("update media_assets set created_at = ? where id = (select cover_media_id from activities where id = ?)", old, activityId);

        maintenance.removeOrphans();

        assertThat(jdbc.queryForObject("select count(*) from media_assets where object_key like 'orphan/old-%'", Long.class)).isZero();
        assertThat(assets.findByObjectKey("orphan/recent")).isPresent();
        assertThat(jdbc.queryForObject("select count(*) from media_assets m join activities a on a.cover_media_id = m.id where a.id = ?",
                Long.class, activityId)).isEqualTo(1);
    }
}
