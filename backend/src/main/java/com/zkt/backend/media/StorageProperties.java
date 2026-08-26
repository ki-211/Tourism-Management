package com.zkt.backend.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.storage")
public record StorageProperties(String type, String localRoot, String publicBaseUrl,
                                String s3Endpoint, String s3Region, String s3Bucket,
                                String s3AccessKey, String s3SecretKey, boolean s3PathStyle) {}
