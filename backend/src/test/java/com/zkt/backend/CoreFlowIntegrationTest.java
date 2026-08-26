package com.zkt.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CoreFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void refreshTokenRotatesAndOldTokenCannotBeReused() throws Exception {
        register("rotate_user");
        JsonNode login = login("rotate_user");
        String oldRefresh = login.path("data").path("refreshToken").asText();
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", oldRefresh))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", oldRefresh))))
                .andExpect(status().isUnauthorized());
    }

    @Test void invitationSignupAndCreatorTransferApplyImmediately() throws Exception {
        register("owner_user"); register("member_user");
        JsonNode ownerLogin = login("owner_user"), memberLogin = login("member_user");
        String ownerToken = ownerLogin.path("data").path("accessToken").asText();
        String memberToken = memberLogin.path("data").path("accessToken").asText();
        long memberId = memberLogin.path("data").path("user").path("id").asLong();

        LocalDateTime now = LocalDateTime.now().withNano(0);
        Map<String, Object> command = Map.of(
                "title", "邀请制郊游", "description", "核心流程测试", "location", "测试校园",
                "signupStart", now.minusMinutes(1).toString(), "signupEnd", now.plusDays(1).toString(),
                "startTime", now.plusDays(2).toString(), "endTime", now.plusDays(3).toString(),
                "visibility", "INVITE_ONLY", "feeRule", "AA"
        );
        JsonNode created = body(mvc.perform(post("/api/v1/activities").header("Authorization", bearer(ownerToken))
                .contentType(MediaType.APPLICATION_JSON).content(json(command))).andExpect(status().isOk()).andReturn());
        long activityId = created.path("data").path("id").asLong();
        String invitationCode = created.path("data").path("invitationCode").asText();

        mvc.perform(get("/api/v1/activities/{id}", activityId).header("Authorization", bearer(memberToken)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/activities/{id}", activityId).param("invitationCode", invitationCode)
                        .header("Authorization", bearer(memberToken))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/activities/{id}/signups", activityId).header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("invitationCode", invitationCode, "passengerCount", 1))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/activities/{id}/signups", activityId).header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("invitationCode", invitationCode))))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/v1/activities/{id}/transfer", activityId).header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("newCreatorId", memberId))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/activities/{id}/signups", activityId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/activities/{id}/signups", activityId).header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk());
    }

    private void register(String username) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", "Password123!", "nickname", username))))
                .andExpect(status().isOk());
    }
    private JsonNode login(String username) throws Exception {
        return body(mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", "Password123!"))))
                .andExpect(status().isOk()).andReturn());
    }
    private JsonNode body(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsByteArray());
    }
    private String json(Object value) throws Exception { return mapper.writeValueAsString(value); }
    private String bearer(String token) { return "Bearer " + token; }
}
