package com.zkt.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkt.backend.location.SharedLocation;
import com.zkt.backend.location.SharedLocationRepository;
import com.zkt.backend.auth.WebSocketTicketService;
import com.zkt.backend.auth.UserRepository;
import com.zkt.backend.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CoreFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired SharedLocationRepository sharedLocations;
    @Autowired WebSocketTicketService webSocketTickets;
    @Autowired UserRepository users;
    @Autowired AuthService authService;

    @Test void websocketTicketCanOnlyBeConsumedOnce() throws Exception {
        register("websocket_user");
        Long userId = users.findByUsername("websocket_user").orElseThrow().getId();
        WebSocketTicketService.IssuedTicket issued = webSocketTickets.issue(userId);
        assertThat(webSocketTickets.consume(issued.ticket())).isEqualTo(userId);
        assertThatThrownBy(() -> webSocketTickets.consume(issued.ticket())).isInstanceOf(IllegalArgumentException.class);
        WebSocketTicketService.IssuedTicket revoked = webSocketTickets.issue(userId);
        authService.logoutAll(userId);
        assertThatThrownBy(() -> webSocketTickets.consume(revoked.ticket())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void deletingAccountAnonymizesUserAndRevokesLogin() throws Exception {
        register("delete_user");
        JsonNode login = login("delete_user");
        String access = login.path("data").path("accessToken").asText();
        mvc.perform(delete("/api/v1/users/me").header("Authorization", bearer(access))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("password", "Password123!"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(access))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "delete_user", "password", "Password123!"))))
                .andExpect(status().isUnauthorized());
        assertThat(users.findByUsername("delete_user")).isEmpty();
    }

    @Test void changingPasswordRevokesAccessAndRefreshTokens() throws Exception {
        register("password_user");
        JsonNode login = login("password_user");
        String access = login.path("data").path("accessToken").asText();
        String refresh = login.path("data").path("refreshToken").asText();
        mvc.perform(put("/api/v1/users/me/password").header("Authorization", bearer(access))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of(
                                "currentPassword", "Password123!", "newPassword", "NewPassword456!"))))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer(access))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refresh))))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "password_user", "password", "NewPassword456!"))))
                .andExpect(status().isOk());
    }

    @Test void privateAlbumUrlRechecksMembershipAndPhotoCanBeDeleted() throws Exception {
        register("media_owner"); register("media_member");
        JsonNode owner = login("media_owner"), member = login("media_member");
        String ownerToken = owner.path("data").path("accessToken").asText();
        String memberToken = member.path("data").path("accessToken").asText();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        long activityId = createActivity(ownerToken, "媒体权限活动", now.minusMinutes(1), now.plusHours(1), now.plusHours(2), now.plusHours(3));
        mvc.perform(post("/api/v1/activities/{id}/signups", activityId).header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        MockMultipartFile image = new MockMultipartFile("file", "photo.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1});
        JsonNode uploaded = body(mvc.perform(multipart("/api/v1/activities/{id}/photos", activityId).file(image)
                        .header("Authorization", bearer(memberToken))).andExpect(status().isOk()).andReturn());
        long photoId = uploaded.path("data").path("id").asLong();
        String signedUrl = uploaded.path("data").path("url").asText();
        java.net.URI uri = java.net.URI.create(signedUrl);
        String token = java.net.URLDecoder.decode(uri.getRawQuery().substring("token=".length()), java.nio.charset.StandardCharsets.UTF_8);
        mvc.perform(get(uri.getPath()).param("token", token)).andExpect(status().isOk());
        mvc.perform(delete("/api/v1/activities/{id}/signups/me", activityId).header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk());
        mvc.perform(get(uri.getPath()).param("token", token)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/activities/{id}/photos/{photoId}", activityId, photoId)
                        .header("Authorization", bearer(ownerToken))).andExpect(status().isOk());
        mvc.perform(get(uri.getPath()).param("token", token)).andExpect(status().isNotFound());
    }

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
                "title", "邀请制活动", "description", "核心流程测试", "location", "测试场地",
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

    @Test void locationSharingEnforcesMembershipExpiryAndActivityEnd() throws Exception {
        register("location_owner"); register("location_member"); register("location_guest");
        JsonNode ownerLogin = login("location_owner"), memberLogin = login("location_member"), outsiderLogin = login("location_guest");
        String ownerToken = ownerLogin.path("data").path("accessToken").asText();
        String memberToken = memberLogin.path("data").path("accessToken").asText();
        String outsiderToken = outsiderLogin.path("data").path("accessToken").asText();
        long ownerId = ownerLogin.path("data").path("user").path("id").asLong();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        long activityId = createActivity(ownerToken, "位置共享活动", now.minusMinutes(1), now.plusHours(1), now.plusHours(2), now.plusHours(3));

        mvc.perform(get("/api/v1/activities/{id}/locations", activityId).header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/activities/{id}/signups", activityId).header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of())))
                .andExpect(status().isOk());

        JsonNode updated = body(mvc.perform(put("/api/v1/activities/{id}/locations/me", activityId)
                        .header("Authorization", bearer(ownerToken)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("latitude", 39.9042, "longitude", 116.4074, "address", "北京市"))))
                .andExpect(status().isOk()).andReturn());
        LocalDateTime expiresAt = LocalDateTime.parse(updated.path("data").path("expiresAt").asText());
        assertThat(Duration.between(LocalDateTime.now(), expiresAt).toSeconds()).isBetween(85L, 90L);

        mvc.perform(put("/api/v1/activities/{id}/locations/me", activityId).header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("latitude", 39.905, "longitude", 116.408))))
                .andExpect(status().isOk());
        JsonNode listed = body(mvc.perform(get("/api/v1/activities/{id}/locations", activityId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andReturn());
        assertThat(listed.path("data").size()).isEqualTo(2);

        mvc.perform(put("/api/v1/activities/{id}/locations/me", activityId).header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("latitude", 91, "longitude", 116.4))))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/activities/{id}/locations/me", activityId).header("Authorization", bearer(outsiderToken))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("latitude", 39.9, "longitude", 116.4))))
                .andExpect(status().isForbidden());
        JsonNode creatorLeave = body(mvc.perform(delete("/api/v1/activities/{id}/signups/me", activityId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isConflict()).andReturn());
        assertThat(creatorLeave.path("code").asText()).isEqualTo("CREATOR_MUST_TRANSFER");

        mvc.perform(delete("/api/v1/activities/{id}/signups/me", activityId).header("Authorization", bearer(memberToken)))
                .andExpect(status().isOk());
        JsonNode afterLeave = body(mvc.perform(get("/api/v1/activities/{id}/locations", activityId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andReturn());
        assertThat(afterLeave.path("data").size()).isEqualTo(1);
        mvc.perform(get("/api/v1/activities/{id}/locations", activityId).header("Authorization", bearer(memberToken)))
                .andExpect(status().isForbidden());

        SharedLocation ownerLocation = sharedLocations.findByActivityIdAndUserId(activityId, ownerId).orElseThrow();
        ownerLocation.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        sharedLocations.save(ownerLocation);
        JsonNode expired = body(mvc.perform(get("/api/v1/activities/{id}/locations", activityId).header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andReturn());
        assertThat(expired.path("data").isEmpty()).isTrue();

        long endedActivity = createActivity(ownerToken, "已结束活动", now.minusDays(4), now.minusDays(3), now.minusDays(2), now.minusDays(1));
        SharedLocation stale = new SharedLocation();
        stale.setActivityId(endedActivity); stale.setUserId(ownerId);
        stale.setLatitude(new java.math.BigDecimal("39.9")); stale.setLongitude(new java.math.BigDecimal("116.4"));
        stale.setExpiresAt(now.plusMinutes(5)); sharedLocations.save(stale);
        JsonNode endedList = body(mvc.perform(get("/api/v1/activities/{id}/locations", endedActivity)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk()).andReturn());
        assertThat(endedList.path("data").isEmpty()).isTrue();
        JsonNode ended = body(mvc.perform(put("/api/v1/activities/{id}/locations/me", endedActivity)
                        .header("Authorization", bearer(ownerToken)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("latitude", 39.9, "longitude", 116.4))))
                .andExpect(status().isConflict()).andReturn());
        assertThat(ended.path("code").asText()).isEqualTo("ACTIVITY_ENDED");
    }

    private long createActivity(String token, String title, LocalDateTime signupStart, LocalDateTime signupEnd,
                                LocalDateTime startTime, LocalDateTime endTime) throws Exception {
        JsonNode created = body(mvc.perform(post("/api/v1/activities").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of(
                        "title", title, "location", "测试场地", "signupStart", signupStart.toString(),
                        "signupEnd", signupEnd.toString(), "startTime", startTime.toString(), "endTime", endTime.toString(),
                        "visibility", "PUBLIC")))).andExpect(status().isOk()).andReturn());
        return created.path("data").path("id").asLong();
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
