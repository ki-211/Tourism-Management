package com.zkt.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ActivityLifecycleIntegrationTest {
    private static final AtomicInteger SEQ = new AtomicInteger();
    @LocalServerPort int port;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void capacityIncludesTheLeaderAndBlocksExtraSignups() throws Exception {
        String owner = user(), first = user(), second = user();
        long id = activity(owner, Map.of("capacity", 2));
        join(first, id).andExpect(status().isOk());
        join(second, id).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ACTIVITY_FULL"));
        JsonNode detail = data(get("/api/v1/activities/{id}", id).header("Authorization", bearer(second)));
        assertThat(detail.path("capacity").asInt()).isEqualTo(2);
        assertThat(detail.path("memberCount").asInt()).isEqualTo(2);
    }

    @Test void concurrentSignupsNeverExceedCapacity() throws Exception {
        String owner = user();
        long id = activity(owner, Map.of("capacity", 4));
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 8; i++) tokens.add(user());
        HttpClient client = HttpClient.newHttpClient();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (String token : tokens) results.add(pool.submit(() -> {
            start.await();
            return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/activities/" + id + "/signups"))
                    .header("Authorization", bearer(token)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}")).build(), HttpResponse.BodyHandlers.discarding()).statusCode();
        }));
        start.countDown();
        List<Integer> codes = new ArrayList<>();
        for (Future<Integer> result : results) codes.add(result.get(30, TimeUnit.SECONDS));
        pool.shutdown();
        assertThat(codes.stream().filter(code -> code == 200)).hasSize(3);
        assertThat(codes.stream().filter(code -> code == 409)).hasSize(5);
        assertThat(data(get("/api/v1/activities/{id}", id).header("Authorization", bearer(owner))).path("memberCount").asInt()).isEqualTo(4);
    }

    @Test void leaderCanEditDetailsVisibilityAndCapacityWithinLimits() throws Exception {
        String owner = user(), member = user();
        long id = activity(owner, Map.of());
        join(member, id).andExpect(status().isOk());

        mvc.perform(put("/api/v1/activities/{id}", id).header("Authorization", bearer(member))
                .contentType(MediaType.APPLICATION_JSON).content(json(command("改名", Map.of())))).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/activities/{id}", id).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(json(command("改名", Map.of("capacity", 2)))))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/activities/{id}", id).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(json(command("改名", Map.of("capacity", 2)))))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/activities/{id}", id).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(json(command("再改", Map.of("capacity", 1)))))
                .andExpect(status().isBadRequest());

        JsonNode invite = data(put("/api/v1/activities/{id}", id).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(json(command("邀请制", Map.of("visibility", "INVITE_ONLY")))));
        assertThat(invite.path("title").asText()).isEqualTo("邀请制");
        assertThat(invite.path("invitationCode").asText()).hasSize(10);
        assertThat(invite.path("capacity").isNull()).isTrue();
        JsonNode back = data(put("/api/v1/activities/{id}", id).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(json(command("公开", Map.of()))));
        assertThat(back.path("invitationCode").isNull()).isTrue();
    }

    @Test void capacityCannotDropBelowCurrentMembers() throws Exception {
        String owner = user(), a = user(), b = user();
        long id = activity(owner, Map.of());
        join(a, id).andExpect(status().isOk());
        join(b, id).andExpect(status().isOk());
        mvc.perform(put("/api/v1/activities/{id}", id).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(json(command("缩容", Map.of("capacity", 2)))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CAPACITY_BELOW_MEMBERS"));
    }

    @Test void cancelledActivitiesBecomeReadOnlyAndLeaveDiscovery() throws Exception {
        String owner = user(), member = user(), outsider = user();
        long id = activity(owner, Map.of());
        join(member, id).andExpect(status().isOk());
        mvc.perform(post("/api/v1/activities/{id}/cancel", id).header("Authorization", bearer(member))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());

        JsonNode cancelled = data(post("/api/v1/activities/{id}/cancel", id).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("reason", "天气原因"))));
        assertThat(cancelled.path("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.path("cancelReason").asText()).isEqualTo("天气原因");

        join(outsider, id).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ACTIVITY_CANCELLED"));
        mvc.perform(post("/api/v1/activities/{id}/messages", id).header("Authorization", bearer(member))
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hi\"}")).andExpect(status().isConflict());
        mvc.perform(put("/api/v1/activities/{id}/locations/me", id).header("Authorization", bearer(member))
                .contentType(MediaType.APPLICATION_JSON).content("{\"latitude\":39.9,\"longitude\":116.4}")).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/activities/{id}/sign-tasks", id).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"集合\"}")).andExpect(status().isConflict());
        mvc.perform(put("/api/v1/activities/{id}", id).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(json(command("复活", Map.of())))).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/activities/{id}/cancel", id).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isConflict());

        JsonNode discover = data(get("/api/v1/activities").param("size", "50").header("Authorization", bearer(outsider))).path("items");
        assertThat(discover.findValues("id").stream().map(JsonNode::asLong)).doesNotContain(id);
        JsonNode joined = data(get("/api/v1/activities").param("scope", "joined").header("Authorization", bearer(member))).path("items");
        assertThat(joined.get(0).path("status").asText()).isEqualTo("CANCELLED");

        mvc.perform(delete("/api/v1/activities/{id}/signups/me", id).header("Authorization", bearer(member))).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/users/me").header("Authorization", bearer(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"Password123!\"}")).andExpect(status().isOk());
    }

    private Map<String, Object> command(String title, Map<String, Object> overrides) {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        Map<String, Object> body = new HashMap<>(Map.of(
                "title", title, "location", "测试场地", "signupStart", now.minusMinutes(1).toString(),
                "signupEnd", now.plusDays(1).toString(), "startTime", now.plusDays(2).toString(),
                "endTime", now.plusDays(3).toString(), "visibility", "PUBLIC"));
        body.putAll(overrides);
        return body;
    }
    private long activity(String token, Map<String, Object> overrides) throws Exception {
        return data(post("/api/v1/activities").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json(command("生命周期", overrides)))).path("id").asLong();
    }
    private ResultActions join(String token, long id) throws Exception {
        return mvc.perform(post("/api/v1/activities/{id}/signups", id).header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{}"));
    }
    private String user() throws Exception {
        String username = "life_" + SEQ.incrementAndGet();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", "Password123!")))).andExpect(status().isOk());
        return data(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", "Password123!")))).path("accessToken").asText();
    }
    private JsonNode data(RequestBuilder request) throws Exception {
        return mapper.readTree(mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data");
    }
    private String json(Object value) throws Exception { return mapper.writeValueAsString(value); }
    private static String bearer(String token) { return "Bearer " + token; }
}
