package com.zkt.backend.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkt.backend.common.DomainException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/** 调用微信 code2Session，用 wx.login 得到的一次性 code 换取 openid。session_key 不落库也不返回前端。 */
@Slf4j
@Component
public class WechatClient {
    private final RestClient client;
    private final ObjectMapper mapper;
    private final WechatProperties properties;

    public WechatClient(RestClient.Builder builder, ObjectMapper mapper, WechatProperties properties) {
        this.client = builder.build();
        this.mapper = mapper;
        this.properties = properties;
    }

    public String openid(String code) {
        if (!properties.configured())
            throw DomainException.badRequest("WECHAT_NOT_CONFIGURED", "服务端尚未配置微信小程序 AppID 与 AppSecret");
        URI uri = UriComponentsBuilder.fromUriString("https://api.weixin.qq.com/sns/jscode2session")
                .queryParam("appid", properties.appId()).queryParam("secret", properties.appSecret())
                .queryParam("js_code", code).queryParam("grant_type", "authorization_code")
                .build().encode().toUri();
        JsonNode root;
        try {
            // 微信该接口返回 text/plain，需要手动解析 JSON
            root = mapper.readTree(client.get().uri(uri).retrieve().body(String.class));
        } catch (RestClientException | java.io.IOException e) {
            log.warn("Wechat code2session unavailable errorType={}", e.getClass().getSimpleName());
            throw DomainException.unavailable("WECHAT_UNAVAILABLE", "微信登录服务暂时不可用，请稍后重试");
        }
        int errcode = root == null ? -1 : root.path("errcode").asInt(0);
        String openid = root == null ? "" : root.path("openid").asText("");
        if (errcode == 40029 || errcode == 40163)
            throw new DomainException(HttpStatus.UNAUTHORIZED, "WECHAT_CODE_INVALID", "微信登录凭证无效或已使用，请重试");
        if (errcode != 0 || openid.isBlank()) {
            log.warn("Wechat code2session rejected providerCode={}", errcode);
            throw DomainException.unavailable("WECHAT_UNAVAILABLE", "微信登录失败，请稍后重试");
        }
        return openid;
    }
}
