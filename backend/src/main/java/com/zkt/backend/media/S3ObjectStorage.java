package com.zkt.backend.media;

import io.minio.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3ObjectStorage implements ObjectStorage {
    private final MinioClient client;
    private final String bucket;

    public S3ObjectStorage(StorageProperties p) {
        if (p.s3Endpoint() == null || p.s3Endpoint().isBlank() || p.s3AccessKey() == null || p.s3AccessKey().isBlank())
            throw new IllegalStateException("S3 配置不完整");
        client = MinioClient.builder().endpoint(p.s3Endpoint()).credentials(p.s3AccessKey(), p.s3SecretKey()).build();
        bucket = p.s3Bucket();
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        } catch (Exception e) { throw new IllegalStateException("S3 存储初始化失败", e); }
    }
    @Override public void put(String key, byte[] content, String type) {
        try { client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).contentType(type)
                .stream(new ByteArrayInputStream(content), content.length, -1).build()); }
        catch (Exception e) { throw new IllegalStateException("图片保存失败", e); }
    }
    @Override public InputStream open(String key) {
        try { return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build()); }
        catch (Exception e) { throw new IllegalStateException("图片读取失败", e); }
    }
    @Override public void delete(String key) {
        try { client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build()); } catch (Exception ignored) {}
    }
}
