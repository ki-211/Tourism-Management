package com.zkt.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.wechat")
public record WechatProperties(String appId, String appSecret) {
    public boolean configured() { return appId != null && !appId.isBlank() && appSecret != null && !appSecret.isBlank(); }
}
