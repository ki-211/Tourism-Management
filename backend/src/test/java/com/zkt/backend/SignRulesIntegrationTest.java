package com.zkt.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SignRulesIntegrationTest {
    private static final AtomicInteger SEQ = new AtomicInteger();
    // Tiananmen-area GCJ-02 point; NEAR is ~99 m away, FAR ~1.3 km.
    private static final double LAT = 39.9087, LON = 116.3975;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test void rejectsInvalidRules() throws Exception {
        String owner = user(); long activity = activity(owner);
        createTask(owner, activity, Map.of("deadline", LocalDateTime.now().minusMinutes(1).withNano(0).toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_DEADLINE"));
        createTask(owner, activity, Map.of("latitude", LAT, "longitude", LON))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_GEOFENCE"));
        createTask(owner, activity, Map.of("latitude", LAT, "longitude", LON, "radiusMeters", 10))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test void geofenceRequiresLocationWithinRadiusAndRecordsDistance() throws Exception {
        String owner = user(), member = user(); long activity = activity(owner); join(member, activity);
        JsonNode task = data(createTask(owner, activity, Map.of("latitude", LAT, "longitude", LON, "radiusMeters", 200, "locationName", "集合点")));
        assertThat(task.path("radiusMeters").asInt()).isEqualTo(200);
        long taskId = task.path("id").asLong();

        sign(member, taskId, Map.of()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("LOCATION_REQUIRED"));
        sign(member, taskId, Map.of("latitude", 39.92, "longitude", 116.40))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("OUT_OF_RANGE"));
        JsonNode record = data(sign(member, taskId, Map.of("latitude", 39.9095, "longitude", 116.3980)));
        assertThat(record.path("distanceMeters").asInt()).isBetween(90, 110);
        JsonNode summary = data(mvc.perform(get("/api/v1/sign-tasks/{id}/summary", taskId).header("Authorization", bearer(owner))));
        assertThat(summary.path("members").findValues("distanceMeters").stream().filter(JsonNode::isInt)).hasSize(1);
    }

    @Test void photoRequiredTasksRejectSignInsWithoutPhoto() throws Exception {
        String owner = user(), member = user(); long activity = activity(owner); join(member, activity);
        long taskId = data(createTask(owner, activity, Map.of("photoRequired", true))).path("id").asLong();
        sign(member, taskId, Map.of()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PHOTO_REQUIRED"));
        mvc.perform(multipart("/api/v1/sign-tasks/{id}/records", taskId)
                        .file(new MockMultipartFile("file", "p.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1}))
                        .header("Authorization", bearer(member)))
                .andExpect(status().isOk());
    }

    @Test void closedTasksRejectSignInsAndLeaveTheTodoList() throws Exception {
        String owner = user(), member = user(); long activity = activity(owner); join(member, activity);
        JsonNode task = data(createTask(owner, activity, Map.of("deadline", LocalDateTime.now().plusHours(1).withNano(0).toString())));
        long taskId = task.path("id").asLong();
        assertThat(task.path("closed").asBoolean()).isFalse();
        assertThat(data(mvc.perform(get("/api/v1/sign-tasks/unsigned").header("Authorization", bearer(member)))).findValues("id")
                .stream().map(JsonNode::asLong)).contains(taskId);

        jdbc.update("update sign_tasks set deadline = ? where id = ?", LocalDateTime.now().minusMinutes(1), taskId);
        sign(member, taskId, Map.of()).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SIGN_CLOSED"));
        assertThat(data(mvc.perform(get("/api/v1/sign-tasks/{id}", taskId).header("Authorization", bearer(member)))).path("closed").asBoolean()).isTrue();
        assertThat(data(mvc.perform(get("/api/v1/sign-tasks/unsigned").header("Authorization", bearer(member)))).findValues("id")
                .stream().map(JsonNode::asLong)).doesNotContain(taskId);
    }

    private ResultActions createTask(String owner, long activity, Map<String, Object> rules) throws Exception {
        Map<String, Object> body = new HashMap<>(rules);
        body.put("title", "集合签到");
        return mvc.perform(post("/api/v1/activities/{id}/sign-tasks", activity).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }
    private ResultActions sign(String member, long taskId, Map<String, Object> body) throws Exception {
        return mvc.perform(post("/api/v1/sign-tasks/{id}/records", taskId).header("Authorization", bearer(member))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)));
    }
    private long activity(String owner) throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        return data(mvc.perform(post("/api/v1/activities").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(
                        "title", "签到规则", "location", "测试场地", "signupStart", now.minusMinutes(1).toString(),
                        "signupEnd", now.plusDays(1).toString(), "startTime", now.plusDays(2).toString(),
                        "endTime", now.plusDays(3).toString(), "visibility", "PUBLIC"))))).path("id").asLong();
    }
    private void join(String member, long activity) throws Exception {
        mvc.perform(post("/api/v1/activities/{id}/signups", activity).header("Authorization", bearer(member))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }
    private String user() throws Exception {
        String username = "rule_" + SEQ.incrementAndGet();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", username, "password", "Password123!")))).andExpect(status().isOk());
        return data(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", username, "password", "Password123!"))))).path("accessToken").asText();
    }
    private JsonNode data(ResultActions result) throws Exception {
        return mapper.readTree(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data");
    }
    private static String bearer(String token) { return "Bearer " + token; }
}
