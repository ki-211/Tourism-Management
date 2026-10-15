package com.zkt.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Guards against N+1 queries: list endpoints must issue the same number of statements for 2 rows and for 5. */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class QueryCountIntegrationTest {
    private static final AtomicInteger SEQ = new AtomicInteger();
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired EntityManagerFactory emf;
    Statistics stats;

    @BeforeEach void statistics() { stats = emf.unwrap(SessionFactory.class).getStatistics(); }

    @Test void createdActivitiesWithCovers() throws Exception {
        User owner = user();
        for (int i = 0; i < 2; i++) cover(owner, activity(owner));
        long small = statements(get("/api/v1/activities").param("scope", "created").header("Authorization", owner.bearer()));
        for (int i = 0; i < 3; i++) cover(owner, activity(owner));
        long large = statements(get("/api/v1/activities").param("scope", "created").header("Authorization", owner.bearer()));
        assertThat(large).isEqualTo(small);
        JsonNode items = data(get("/api/v1/activities").param("scope", "created").header("Authorization", owner.bearer())).path("items");
        assertThat(items).hasSize(5).allSatisfy(item -> {
            assertThat(item.path("joined").asBoolean()).isTrue();
            assertThat(item.path("creator").asBoolean()).isTrue();
            assertThat(item.path("coverUrl").asText()).contains("/media/public/");
        });
    }

    @Test void memberList() throws Exception {
        User owner = user(); long activity = activity(owner);
        for (int i = 0; i < 2; i++) join(user(), activity);
        long small = statements(get("/api/v1/activities/{id}/signups", activity).header("Authorization", owner.bearer()));
        for (int i = 0; i < 3; i++) join(user(), activity);
        long large = statements(get("/api/v1/activities/{id}/signups", activity).header("Authorization", owner.bearer()));
        assertThat(large).isEqualTo(small);
    }

    @Test void albumAndChatAndLocations() throws Exception {
        User owner = user(); long activity = activity(owner);
        for (int i = 0; i < 2; i++) memberActivity(user(), activity);
        long photos = statements(get("/api/v1/activities/{id}/photos", activity).header("Authorization", owner.bearer()));
        long messages = statements(get("/api/v1/activities/{id}/messages", activity).header("Authorization", owner.bearer()));
        long locations = statements(get("/api/v1/activities/{id}/locations", activity).header("Authorization", owner.bearer()));
        for (int i = 0; i < 3; i++) memberActivity(user(), activity);
        assertThat(statements(get("/api/v1/activities/{id}/photos", activity).header("Authorization", owner.bearer()))).isEqualTo(photos);
        assertThat(statements(get("/api/v1/activities/{id}/messages", activity).header("Authorization", owner.bearer()))).isEqualTo(messages);
        assertThat(statements(get("/api/v1/activities/{id}/locations", activity).header("Authorization", owner.bearer()))).isEqualTo(locations);
        assertThat(data(get("/api/v1/activities/{id}/photos", activity).header("Authorization", owner.bearer())))
                .hasSize(5).allSatisfy(photo -> assertThat(photo.path("deletable").asBoolean()).isTrue());
        assertThat(data(get("/api/v1/activities/{id}/locations", activity).header("Authorization", owner.bearer()))).hasSize(5);
    }

    @Test void signTasksSummaryAndHistory() throws Exception {
        User owner = user(), member = user(); long activity = activity(owner);
        join(member, activity);
        long firstTask = task(owner, activity);
        for (int i = 0; i < 2; i++) { User signer = user(); join(signer, activity); sign(signer, firstTask, i == 0); }
        for (int i = 0; i < 2; i++) sign(member, task(owner, activity), false);
        long summary = statements(get("/api/v1/sign-tasks/{id}/summary", firstTask).header("Authorization", owner.bearer()));
        long tasks = statements(get("/api/v1/activities/{id}/sign-tasks", activity).header("Authorization", member.bearer()));
        long unsigned = statements(get("/api/v1/sign-tasks/unsigned").header("Authorization", member.bearer()));
        long history = statements(get("/api/v1/users/me/sign-records").header("Authorization", member.bearer()));

        for (int i = 0; i < 3; i++) { User signer = user(); join(signer, activity); sign(signer, firstTask, i == 0); }
        for (int i = 0; i < 3; i++) {
            long other = activity(owner); join(member, other);
            sign(member, task(owner, other), false);
            task(owner, other);
        }
        assertThat(statements(get("/api/v1/sign-tasks/{id}/summary", firstTask).header("Authorization", owner.bearer()))).isEqualTo(summary);
        assertThat(statements(get("/api/v1/activities/{id}/sign-tasks", activity).header("Authorization", member.bearer()))).isEqualTo(tasks);
        assertThat(statements(get("/api/v1/sign-tasks/unsigned").header("Authorization", member.bearer()))).isEqualTo(unsigned);
        assertThat(statements(get("/api/v1/users/me/sign-records").header("Authorization", member.bearer()))).isEqualTo(history);

        JsonNode memberTasks = data(get("/api/v1/activities/{id}/sign-tasks", activity).header("Authorization", member.bearer()));
        assertThat(memberTasks.findValues("signed").stream().filter(JsonNode::asBoolean)).hasSize(2);
        JsonNode summaryBody = data(get("/api/v1/sign-tasks/{id}/summary", firstTask).header("Authorization", owner.bearer()));
        assertThat(summaryBody.path("task").path("signedCount").asLong()).isEqualTo(5);
        assertThat(summaryBody.path("members")).hasSize(7);
        assertThat(summaryBody.path("members").findValues("photoUrl").stream().filter(url -> url.asText().contains("/media/access/"))).hasSize(2);
        assertThat(data(get("/api/v1/users/me/sign-records").header("Authorization", member.bearer()))).hasSize(5);
        assertThat(data(get("/api/v1/sign-tasks/unsigned").header("Authorization", member.bearer()))).hasSize(4);
    }

    private long statements(RequestBuilder request) throws Exception {
        stats.clear();
        mvc.perform(request).andExpect(status().isOk());
        return stats.getPrepareStatementCount();
    }

    private JsonNode data(RequestBuilder request) throws Exception {
        return json(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data");
    }

    private void memberActivity(User member, long activity) throws Exception {
        join(member, activity);
        mvc.perform(multipart("/api/v1/activities/{id}/photos", activity).file(png()).header("Authorization", member.bearer()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/activities/{id}/messages", activity).header("Authorization", member.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hi\"}")).andExpect(status().isOk());
        mvc.perform(put("/api/v1/activities/{id}/locations/me", activity).header("Authorization", member.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":39.9,\"longitude\":116.4}")).andExpect(status().isOk());
    }

    private long activity(User owner) throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        return json(mvc.perform(post("/api/v1/activities").header("Authorization", owner.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(
                        "title", "查询计数", "location", "测试场地", "signupStart", now.minusMinutes(1).toString(),
                        "signupEnd", now.plusDays(1).toString(), "startTime", now.plusDays(2).toString(),
                        "endTime", now.plusDays(3).toString(), "visibility", "PUBLIC"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data").path("id").asLong();
    }
    private void cover(User owner, long activity) throws Exception {
        mvc.perform(multipart("/api/v1/activities/{id}/cover", activity).file(png()).header("Authorization", owner.bearer()))
                .andExpect(status().isOk());
    }
    private void join(User member, long activity) throws Exception {
        mvc.perform(post("/api/v1/activities/{id}/signups", activity).header("Authorization", member.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }
    private long task(User owner, long activity) throws Exception {
        return json(mvc.perform(post("/api/v1/activities/{id}/sign-tasks", activity).header("Authorization", owner.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"集合\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data").path("id").asLong();
    }
    private void sign(User member, long task, boolean withPhoto) throws Exception {
        if (withPhoto) mvc.perform(multipart("/api/v1/sign-tasks/{id}/records", task).file(png()).header("Authorization", member.bearer()))
                .andExpect(status().isOk());
        else mvc.perform(post("/api/v1/sign-tasks/{id}/records", task).header("Authorization", member.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
    }
    private User user() throws Exception {
        String username = "qc_" + SEQ.incrementAndGet();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", username, "password", "Password123!")))).andExpect(status().isOk());
        JsonNode login = json(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", username, "password", "Password123!"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data");
        return new User(login.path("accessToken").asText(), login.path("user").path("id").asLong());
    }
    private static MockMultipartFile png() {
        return new MockMultipartFile("file", "p.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1});
    }
    private JsonNode json(byte[] content) throws Exception { return mapper.readTree(content); }
    private record User(String token, long id) { String bearer() { return "Bearer " + token; } }
}
