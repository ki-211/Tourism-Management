package com.zkt.backend.media;

public interface ObjectStorage {
    void put(String key, byte[] content, String contentType);
    StoredObject get(String key);
    void delete(String key);
    record StoredObject(byte[] content, String contentType) {}
}
