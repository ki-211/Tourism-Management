package com.zkt.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class LoginLockIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test void usernameIsLockedAfterRepeatedFailuresEvenWithTheRightPassword() throws Exception {
        register("lock_target"); register("lock_bystander");
        for (int i = 0; i < 10; i++) login("lock_target", "WrongPassword!").andExpect(status().isUnauthorized());
        login("lock_target", "Password123!").andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("LOGIN_LOCKED"));
        login("lock_bystander", "Password123!").andExpect(status().isOk());
    }

    @Test void successfulLoginResetsTheFailureCount() throws Exception {
        register("lock_reset");
        for (int i = 0; i < 9; i++) login("lock_reset", "WrongPassword!").andExpect(status().isUnauthorized());
        login("lock_reset", "Password123!").andExpect(status().isOk());
        for (int i = 0; i < 9; i++) login("lock_reset", "WrongPassword!").andExpect(status().isUnauthorized());
        login("lock_reset", "Password123!").andExpect(status().isOk());
    }

    @Test void unknownUsernamesAreThrottledLikeRealOnes() throws Exception {
        for (int i = 0; i < 10; i++) login("lock_ghost", "WrongPassword!").andExpect(status().isUnauthorized());
        login("lock_ghost", "WrongPassword!").andExpect(status().isTooManyRequests());
    }

    private void register(String username) throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", username, "password", "Password123!")))).andExpect(status().isOk());
    }
    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", username, "password", password))));
    }
}
