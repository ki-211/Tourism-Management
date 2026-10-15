package com.zkt.backend.media;

import com.zkt.backend.common.DomainException;
import com.zkt.backend.auth.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MediaService {
    private static final long MAX = 10 * 1024 * 1024;
    public static final String THUMBNAIL_CONTENT_TYPE = "image/jpeg";
    private final MediaAssetRepository assets;
    private final ObjectStorage storage;
    private final String publicBaseUrl;
    private final JwtService jwt;
    private final ThumbnailGenerator thumbnails;

    public MediaService(MediaAssetRepository assets, ObjectStorage storage, StorageProperties properties, JwtService jwt,
                        ThumbnailGenerator thumbnails) {
        this.assets = assets; this.storage = storage;
        this.publicBaseUrl = properties.publicBaseUrl().replaceAll("/$", "");
        this.jwt = jwt;
        this.thumbnails = thumbnails;
    }

    @Transactional
    public MediaAsset saveImage(Long ownerId, String purpose, MultipartFile file) {
        if (file == null || file.isEmpty()) throw DomainException.badRequest("EMPTY_FILE", "请选择图片");
        if (file.getSize() > MAX) throw DomainException.badRequest("FILE_TOO_LARGE", "图片不能超过10MB");
        List<String> written = new ArrayList<>();
        try {
            byte[] bytes = file.getBytes();
            Type type = detect(bytes);
            Optional<byte[]> thumbnail = thumbnails.create(bytes);
            String base = UUID.randomUUID().toString().replace("-", "");
            String key = purpose.toLowerCase() + "/" + base + type.extension;
            storage.put(key, bytes, type.mime); written.add(key);
            String thumbnailKey = null;
            if (thumbnail.isPresent()) {
                thumbnailKey = purpose.toLowerCase() + "/thumb/" + base + ".jpg";
                storage.put(thumbnailKey, thumbnail.get(), THUMBNAIL_CONTENT_TYPE); written.add(thumbnailKey);
            }
            MediaAsset asset = new MediaAsset(); asset.setOwnerId(ownerId); asset.setObjectKey(key); asset.setThumbnailKey(thumbnailKey);
            asset.setOriginalName(file.getOriginalFilename() == null ? "image" + type.extension : file.getOriginalFilename());
            asset.setContentType(type.mime); asset.setSizeBytes((long) bytes.length); asset.setPurpose(purpose);
            MediaAsset saved = assets.save(asset);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                List<String> uploaded = List.copyOf(written);
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_COMMITTED) uploaded.forEach(storage::delete);
                    }
                });
            }
            return saved;
        } catch (DomainException e) { written.forEach(storage::delete); throw e; }
        catch (Exception e) { written.forEach(storage::delete); throw new IllegalStateException("图片上传失败", e); }
    }

    /** {@code thumbnailUrl} falls back to the original for assets uploaded before thumbnails existed. */
    public MediaView view(MediaAsset asset, Long viewerId) {
        String path = "COVER".equals(asset.getPurpose())
                ? "/api/v1/media/public/" + asset.getId()
                : "/api/v1/media/access/" + asset.getId() + "?token=" + jwt.createMediaToken(viewerId, asset.getId());
        String url = publicBaseUrl + path;
        String thumbnailUrl = asset.getThumbnailKey() == null ? url : url + (path.contains("?") ? "&" : "?") + "variant=thumb";
        return new MediaView(asset.getId(), url, thumbnailUrl, asset.getContentType(), asset.getSizeBytes());
    }
    public InputStream open(MediaAsset asset, boolean thumbnail) {
        return storage.open(thumbnail && asset.getThumbnailKey() != null ? asset.getThumbnailKey() : asset.getObjectKey());
    }
    public MediaAsset find(Long id) { return assets.findById(id).orElseThrow(() -> DomainException.notFound("图片不存在")); }

    public void removeAfterCommit(MediaAsset asset) {
        assets.delete(asset);
        Runnable deleteObject = () -> {
            storage.delete(asset.getObjectKey());
            if (asset.getThumbnailKey() != null) storage.delete(asset.getThumbnailKey());
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { deleteObject.run(); }
            });
        } else deleteObject.run();
    }

    private Type detect(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8 && (b[2] & 0xff) == 0xff) return Type.JPEG;
        if (b.length >= 8 && (b[0] & 0xff) == 0x89 && b[1] == 0x50 && b[2] == 0x4e && b[3] == 0x47) return Type.PNG;
        if (b.length >= 12 && new String(b, 0, 4).equals("RIFF") && new String(b, 8, 4).equals("WEBP")) return Type.WEBP;
        throw DomainException.badRequest("UNSUPPORTED_IMAGE", "仅支持 JPEG、PNG、WebP 图片");
    }
    private enum Type { JPEG("image/jpeg", ".jpg"), PNG("image/png", ".png"), WEBP("image/webp", ".webp");
        final String mime, extension; Type(String mime, String extension) { this.mime = mime; this.extension = extension; }}
    public record MediaView(Long id, String url, String thumbnailUrl, String contentType, long size) {}
}
