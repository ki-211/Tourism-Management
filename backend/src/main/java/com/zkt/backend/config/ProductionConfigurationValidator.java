package com.zkt.backend.config;

import com.zkt.backend.auth.AppSecurityProperties;
import com.zkt.backend.auth.WechatProperties;
import com.zkt.backend.media.StorageProperties;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@Profile("prod")
public class ProductionConfigurationValidator implements SmartInitializingSingleton {
    private final AppSecurityProperties security;
    private final CorsProperties cors;
    private final StorageProperties storage;
    private final String amapKey;
    private final WechatProperties wechat;

    public ProductionConfigurationValidator(AppSecurityProperties security, CorsProperties cors, StorageProperties storage,
            @Value("${app.amap-web-key:}") String amapKey, WechatProperties wechat) {
        this.security = security;
        this.cors = cors;
        this.storage = storage;
        this.amapKey = amapKey;
        this.wechat = wechat;
    }

    @Override
    public void afterSingletonsInstantiated() {
        require(security.secret(), "JWT_SECRET");
        try {
            if (Decoders.BASE64.decode(security.secret()).length < 32) throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new IllegalStateException("JWT_SECRET 必须是至少 32 字节随机值的 Base64", e);
        }
        if (cors.allowedOrigins() == null || cors.allowedOrigins().isEmpty())
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS 不能为空");
        cors.allowedOrigins().forEach(origin -> requireHttps(origin, "CORS_ALLOWED_ORIGINS"));
        requireHttps(storage.publicBaseUrl(), "PUBLIC_BASE_URL");
        if (!"s3".equalsIgnoreCase(storage.type())) throw new IllegalStateException("生产环境 STORAGE_TYPE 必须为 s3");
        require(storage.s3Endpoint(), "S3_ENDPOINT");
        require(storage.s3Bucket(), "S3_BUCKET");
        require(storage.s3AccessKey(), "S3_ACCESS_KEY");
        require(storage.s3SecretKey(), "S3_SECRET_KEY");
        require(amapKey, "AMAP_WEB_KEY");
        // 微信登录可选；一旦配置 AppID 就必须同时配置 AppSecret
        if (wechat.appId() != null && !wechat.appId().isBlank()) require(wechat.appSecret(), "WECHAT_APP_SECRET");
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " 未配置");
    }

    private static void requireHttps(String value, String name) {
        require(value, name);
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null)
            throw new IllegalStateException(name + " 必须使用有效 HTTPS 地址");
    }
}
