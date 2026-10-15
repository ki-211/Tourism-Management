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
import org.springframework.test.web.servlet.ResultActions;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class VehicleSeatsIntegrationTest {
    private static final AtomicInteger SEQ = new AtomicInteger();
    @LocalServerPort int port;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    record User(String token, long id) {}

    @Test void seatsCountRegisteredRidersAndMembersCanSwitchVehicles() throws Exception {
        User owner = user(), family = user(), solo = user(), late = user();
        long activity = activity(owner);
        join(family, activity, 2); join(solo, activity, null); join(late, activity, null);
        long first = vehicle(owner, activity, 3), second = vehicle(owner, activity, 4);

        JsonNode afterFamily = data(board(family, activity, first, null));
        assertThat(afterFamily.path("occupiedSeats").asInt()).isEqualTo(2);
        assertThat(afterFamily.path("mine").asBoolean()).isTrue();
        assertThat(data(board(solo, activity, first, null)).path("occupiedSeats").asInt()).isEqualTo(3);
        board(late, activity, first, null).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VEHICLE_FULL"));

        assertThat(data(board(solo, activity, second, null)).path("occupiedSeats").asInt()).isEqualTo(1);
        JsonNode list = data(mvc.perform(get("/api/v1/activities/{a}/vehicles", activity).header("Authorization", bearer(family))));
        Map<Long, Integer> occupied = new HashMap<>();
        list.forEach(v -> occupied.put(v.path("id").asLong(), v.path("occupiedSeats").asInt()));
        assertThat(occupied).containsEntry(first, 2).containsEntry(second, 1);
        assertThat(data(board(late, activity, first, null)).path("passengers").findValues("nickname")).hasSize(2);
    }

    @Test void onlyTheLeaderManagesOtherMembersAndVehicles() throws Exception {
        User owner = user(), a = user(), b = user(), outsider = user();
        long activity = activity(owner);
        join(a, activity, null); join(b, activity, null);
        long car = vehicle(owner, activity, 2);

        board(a, activity, car, b.id()).andExpect(status().isForbidden());
        board(outsider, activity, car, null).andExpect(status().isForbidden());
        board(owner, activity, car, outsider.id()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NOT_MEMBER"));
        data(board(owner, activity, car, a.id()));
        data(board(owner, activity, car, b.id()));
        mvc.perform(delete("/api/v1/activities/{a}/vehicles/{v}/passengers/{u}", activity, car, b.id()).header("Authorization", bearer(a)))
                .andExpect(status().isForbidden());

        mvc.perform(put("/api/v1/activities/{a}/vehicles/{v}", activity, car).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(json(vehicleBody(1))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SEATS_BELOW_OCCUPIED"));
        mvc.perform(put("/api/v1/activities/{a}/vehicles/{v}", activity, car).header("Authorization", bearer(a))
                .contentType(MediaType.APPLICATION_JSON).content(json(vehicleBody(6)))).andExpect(status().isForbidden());
        assertThat(data(mvc.perform(delete("/api/v1/activities/{a}/vehicles/{v}/passengers/{u}", activity, car, b.id())
                .header("Authorization", bearer(b)))).path("occupiedSeats").asInt()).isEqualTo(1);

        mvc.perform(delete("/api/v1/activities/{id}/signups/me", activity).header("Authorization", bearer(a))).andExpect(status().isOk());
        assertThat(data(mvc.perform(get("/api/v1/activities/{a}/vehicles", activity).header("Authorization", bearer(owner))))
                .get(0).path("occupiedSeats").asInt()).as("leaving frees the seat").isZero();

        mvc.perform(delete("/api/v1/activities/{a}/vehicles/{v}", activity, car).header("Authorization", bearer(owner))).andExpect(status().isOk());
        assertThat(data(mvc.perform(get("/api/v1/activities/{a}/vehicles", activity).header("Authorization", bearer(owner))))).isEmpty();
    }

    @Test void concurrentBoardingNeverOverfillsAVehicle() throws Exception {
        User owner = user();
        long activity = activity(owner);
        long car = vehicle(owner, activity, 2);
        List<User> riders = new ArrayList<>();
        for (int i = 0; i < 6; i++) { User rider = user(); join(rider, activity, null); riders.add(rider); }
        HttpClient client = HttpClient.newHttpClient();
        ExecutorService pool = Executors.newFixedThreadPool(riders.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (User rider : riders) results.add(pool.submit(() -> {
            start.await();
            return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/activities/" + activity + "/vehicles/" + car + "/passengers"))
                    .header("Authorization", bearer(rider)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                    HttpResponse.BodyHandlers.discarding()).statusCode();
        }));
        start.countDown();
        List<Integer> codes = new ArrayList<>();
        for (Future<Integer> result : results) codes.add(result.get(30, TimeUnit.SECONDS));
        pool.shutdown();
        assertThat(codes.stream().filter(code -> code == 200)).hasSize(2);
        assertThat(codes.stream().filter(code -> code == 409)).hasSize(4);
        assertThat(data(mvc.perform(get("/api/v1/activities/{a}/vehicles", activity).header("Authorization", bearer(owner))))
                .get(0).path("occupiedSeats").asInt()).isEqualTo(2);
    }

    private Map<String, Object> vehicleBody(int seats) {
        return Map.of("plateNumber", "京A12345", "driverName", "王师傅", "pickupTime", LocalDateTime.now().plusDays(2).withNano(0).toString(),
                "pickupLocation", "东门", "seats", seats);
    }
    private long vehicle(User owner, long activity, int seats) throws Exception {
        return data(mvc.perform(post("/api/v1/activities/{a}/vehicles", activity).header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(json(vehicleBody(seats))))).path("id").asLong();
    }
    private ResultActions board(User actor, long activity, long vehicle, Long target) throws Exception {
        return mvc.perform(post("/api/v1/activities/{a}/vehicles/{v}/passengers", activity, vehicle).header("Authorization", bearer(actor))
                .contentType(MediaType.APPLICATION_JSON).content(target == null ? "{}" : json(Map.of("userId", target))));
    }
    private long activity(User owner) throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        return data(mvc.perform(post("/api/v1/activities").header("Authorization", bearer(owner))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of(
                        "title", "车辆座位", "location", "测试场地", "signupStart", now.minusMinutes(1).toString(),
                        "signupEnd", now.plusDays(1).toString(), "startTime", now.plusDays(2).toString(),
                        "endTime", now.plusDays(3).toString(), "visibility", "PUBLIC"))))).path("id").asLong();
    }
    private void join(User member, long activity, Integer riders) throws Exception {
        mvc.perform(post("/api/v1/activities/{id}/signups", activity).header("Authorization", bearer(member))
                .contentType(MediaType.APPLICATION_JSON).content(riders == null ? "{}" : json(Map.of("passengerCount", riders))))
                .andExpect(status().isOk());
    }
    private User user() throws Exception {
        String username = "seat_" + SEQ.incrementAndGet();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", "Password123!")))).andExpect(status().isOk());
        JsonNode login = data(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", "Password123!")))));
        return new User(login.path("accessToken").asText(), login.path("user").path("id").asLong());
    }
    private JsonNode data(ResultActions result) throws Exception {
        return mapper.readTree(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data");
    }
    private String json(Object value) throws Exception { return mapper.writeValueAsString(value); }
    private static String bearer(User user) { return "Bearer " + user.token(); }
}
