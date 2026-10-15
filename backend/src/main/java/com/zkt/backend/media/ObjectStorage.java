package com.zkt.backend.media;

import java.io.InputStream;

public interface ObjectStorage {
    void put(String key, byte[] content, String contentType);
    /** Opens the object for streaming; the caller closes the stream. Content type and size come from {@link MediaAsset}. */
    InputStream open(String key);
    void delete(String key);
}
