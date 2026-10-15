package com.zkt.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkt.backend.auth.WebSocketTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RoomWebSocketIntegrationTest {
    @LocalServerPort int port;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired WebSocketTicketService tickets;

    @Test void memberReceivesCommittedEventsAndIsDisconnectedAfterLeaving() throws Exception {
        String owner = token("ws_owner"), member = token("ws_member");
        long memberId = me(member);
        LocalDateTime now = LocalDateTime.now().withNano(0);
        long activityId = body(mvc.perform(post("/api/v1/activities").header("Authorization", "Bearer " + owner)
                .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of(
                        "title", "实时测试", "location", "测试场地", "signupStart", now.minusMinutes(1).toString(),
                        "signupEnd", now.plusDays(1).toString(), "startTime", now.plusDays(2).toString(),
                        "endTime", now.plusDays(3).toString(), "visibility", "PUBLIC"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data").path("id").asLong();
        mvc.perform(post("/api/v1/activities/{id}/signups", activityId).header("Authorization", "Bearer " + member)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());

        BlockingQueue<JsonNode> received = new LinkedBlockingQueue<>();
        CompletableFuture<CloseStatus> closed = new CompletableFuture<>();
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.setOrigin("http://localhost:5173");
        String ticket = tickets.issue(memberId).ticket();
        WebSocketSession socket = new StandardWebSocketClient().execute(new TextWebSocketHandler() {
            @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                received.add(mapper.readTree(message.getPayload()));
            }
            @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { closed.complete(status); }
        }, headers, URI.create("ws://localhost:" + port + "/ws?ticket=" + ticket)).get(5, TimeUnit.SECONDS);

        socket.sendMessage(new TextMessage("{\"type\":\"SUBSCRIBE_ACTIVITY\",\"activityId\":" + activityId + "}"));
        assertThat(received.poll(5, TimeUnit.SECONDS).path("type").asText()).isEqualTo("SUBSCRIBED");

        mvc.perform(post("/api/v1/activities/{id}/messages", activityId).header("Authorization", "Bearer " + owner)
                .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"hello\"}")).andExpect(status().isOk());
        JsonNode chat = received.poll(5, TimeUnit.SECONDS);
        assertThat(chat.path("type").asText()).isEqualTo("CHAT_CREATED");
        assertThat(chat.path("payload").path("content").asText()).isEqualTo("hello");

        mvc.perform(delete("/api/v1/activities/{id}/signups/me", activityId).header("Authorization", "Bearer " + member))
                .andExpect(status().isOk());
        assertThat(closed.get(5, TimeUnit.SECONDS).getCode()).isEqualTo(4403);
    }

    private String token(String username) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", username, "password", "Password123!"))))
                .andExpect(status().isOk());
        return body(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", username, "password", "Password123!"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray()).path("data").path("accessToken").asText();
    }
    private long me(String token) throws Exception {
        return body(mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsByteArray()).path("data").path("id").asLong();
    }
    private JsonNode body(byte[] content) throws Exception { return mapper.readTree(content); }
}
