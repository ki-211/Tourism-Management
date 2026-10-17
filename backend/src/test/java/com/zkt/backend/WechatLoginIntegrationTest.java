package com.zkt.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkt.backend.auth.WechatClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class WechatLoginIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean WechatClient wechat;

    @Test void firstLoginCreatesAccountAndLaterLoginReusesIt() throws Exception {
        given(wechat.openid("code-1")).willReturn("openid-reuse");
        given(wechat.openid("code-2")).willReturn("openid-reuse");
        JsonNode first = wechatLogin("code-1").path("data").path("user");
        JsonNode second = wechatLogin("code-2").path("data").path("user");
        assertThat(first.path("id").asLong()).isEqualTo(second.path("id").asLong());
        assertThat(first.path("username").asText()).startsWith("wx_").hasSizeLessThanOrEqualTo(16);
        assertThat(first.path("passwordSet").asBoolean()).isFalse();
        assertThat(first.path("wechatBound").asBoolean()).isTrue();
    }

    @Test void wechatUserCanSetPasswordWithoutCurrentPasswordThenLoginByUsername() throws Exception {
        given(wechat.openid("code-pw")).willReturn("openid-password");
        JsonNode data = wechatLogin("code-pw").path("data");
        String username = data.path("user").path("username").asText();
        mvc.perform(put("/api/v1/users/me/password").header("Authorization", "Bearer " + data.path("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("newPassword", "Password123!"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", username, "password", "Password123!"))))
                .andExpect(status().isOk());
        given(wechat.openid("code-pw-2")).willReturn("openid-password");
        JsonNode user = wechatLogin("code-pw-2").path("data").path("user");
        assertThat(user.path("passwordSet").asBoolean()).isTrue();
        mvc.perform(put("/api/v1/users/me/password").header("Authorization", "Bearer " + wechatLogin("code-pw-3", "openid-password"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("newPassword", "Another456!"))))
                .andExpect(status().isUnauthorized());
    }

    @Test void deletedWechatAccountReleasesOpenid() throws Exception {
        given(wechat.openid("code-del")).willReturn("openid-delete");
        JsonNode data = wechatLogin("code-del").path("data");
        mvc.perform(delete("/api/v1/users/me").header("Authorization", "Bearer " + data.path("accessToken").asText()))
                .andExpect(status().isOk());
        given(wechat.openid("code-del-2")).willReturn("openid-delete");
        JsonNode again = wechatLogin("code-del-2").path("data").path("user");
        assertThat(again.path("id").asLong()).isNotEqualTo(data.path("user").path("id").asLong());
    }

    private String wechatLogin(String code, String openid) throws Exception {
        given(wechat.openid(code)).willReturn(openid);
        return wechatLogin(code).path("data").path("accessToken").asText();
    }

    private JsonNode wechatLogin(String code) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/wechat").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("code", code))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    private String json(Object value) throws Exception { return mapper.writeValueAsString(value); }
}
