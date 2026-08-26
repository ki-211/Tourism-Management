package com.zkt.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties("app.jwt")
public record AppSecurityProperties(String secret, Duration accessTtl, Duration refreshTtl) {}
