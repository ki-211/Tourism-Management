package com.zkt.backend.media;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalObjectStorage implements ObjectStorage {
    private final Path root;
    public LocalObjectStorage(StorageProperties props) {
        root = Path.of(props.localRoot()).toAbsolutePath().normalize();
        try { Files.createDirectories(root); } catch (Exception e) { throw new IllegalStateException("无法创建本地存储目录", e); }
    }
    @Override public void put(String key, byte[] content, String type) {
        try {
            Path target = root.resolve(key).normalize();
            if (!target.startsWith(root)) throw new IllegalArgumentException("非法对象路径");
            Files.createDirectories(target.getParent()); Files.write(target, content);
        } catch (Exception e) { throw new IllegalStateException("图片保存失败", e); }
    }
    @Override public InputStream open(String key) {
        try { return Files.newInputStream(root.resolve(key)); }
        catch (Exception e) { throw new IllegalStateException("图片读取失败", e); }
    }
    @Override public void delete(String key) {
        try { Files.deleteIfExists(root.resolve(key)); Files.deleteIfExists(root.resolve(key + ".type")); }
        catch (Exception ignored) {}
    }
}
