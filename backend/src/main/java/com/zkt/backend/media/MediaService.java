package com.zkt.backend.media;

import com.zkt.backend.common.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@Service
public class MediaService {
    private static final long MAX = 10 * 1024 * 1024;
    private final MediaAssetRepository assets;
    private final ObjectStorage storage;
    private final String publicBaseUrl;

    public MediaService(MediaAssetRepository assets, ObjectStorage storage, StorageProperties properties) {
        this.assets = assets; this.storage = storage;
        this.publicBaseUrl = properties.publicBaseUrl().replaceAll("/$", "");
    }

    @Transactional
    public MediaAsset saveImage(Long ownerId, String purpose, MultipartFile file) {
        if (file == null || file.isEmpty()) throw DomainException.badRequest("EMPTY_FILE", "请选择图片");
        if (file.getSize() > MAX) throw DomainException.badRequest("FILE_TOO_LARGE", "图片不能超过10MB");
        try {
            byte[] bytes = file.getBytes();
            Type type = detect(bytes);
            String key = purpose.toLowerCase() + "/" + UUID.randomUUID().toString().replace("-", "") + type.extension;
            storage.put(key, bytes, type.mime);
            MediaAsset asset = new MediaAsset(); asset.setOwnerId(ownerId); asset.setObjectKey(key);
            asset.setOriginalName(file.getOriginalFilename() == null ? "image" + type.extension : file.getOriginalFilename());
            asset.setContentType(type.mime); asset.setSizeBytes((long) bytes.length); asset.setPurpose(purpose);
            try { return assets.save(asset); } catch (Exception e) { storage.delete(key); throw e; }
        } catch (DomainException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("图片上传失败", e); }
    }

    public MediaView view(MediaAsset asset) {
        return new MediaView(asset.getId(), publicBaseUrl + "/api/v1/media/public/" + asset.getObjectKey(), asset.getContentType(), asset.getSizeBytes());
    }
    public ObjectStorage.StoredObject load(String objectKey) {
        assets.findByObjectKey(objectKey).orElseThrow(() -> DomainException.notFound("图片不存在"));
        return storage.get(objectKey);
    }

    private Type detect(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8 && (b[2] & 0xff) == 0xff) return Type.JPEG;
        if (b.length >= 8 && (b[0] & 0xff) == 0x89 && b[1] == 0x50 && b[2] == 0x4e && b[3] == 0x47) return Type.PNG;
        if (b.length >= 12 && new String(b, 0, 4).equals("RIFF") && new String(b, 8, 4).equals("WEBP")) return Type.WEBP;
        throw DomainException.badRequest("UNSUPPORTED_IMAGE", "仅支持 JPEG、PNG、WebP 图片");
    }
    private enum Type { JPEG("image/jpeg", ".jpg"), PNG("image/png", ".png"), WEBP("image/webp", ".webp");
        final String mime, extension; Type(String mime, String extension) { this.mime = mime; this.extension = extension; }}
    public record MediaView(Long id, String url, String contentType, long size) {}
}
