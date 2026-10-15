package com.zkt.backend.media;

import com.zkt.backend.auth.JwtService;
import com.zkt.backend.common.DomainException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {
    // Media ids and object keys are never reused, so a cover's bytes for an id never change.
    private static final CacheControl PUBLIC_CACHE = CacheControl.maxAge(Duration.ofDays(7)).cachePublic();
    // Signed URLs are per viewer and expire in 5 minutes; let only the viewer's own client reuse them meanwhile.
    private static final CacheControl PRIVATE_CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate();
    private final MediaService media;
    private final MediaAuthorizationService authorization;
    private final JwtService jwt;

    public MediaController(MediaService media, MediaAuthorizationService authorization, JwtService jwt) {
        this.media = media; this.authorization = authorization; this.jwt = jwt;
    }

    @GetMapping("/public/{id}")
    ResponseEntity<Resource> publicCover(@PathVariable Long id, @RequestParam(required = false) String variant) {
        MediaAsset asset = media.find(id);
        if (!"COVER".equals(asset.getPurpose())) throw DomainException.forbidden("该图片不是公开封面");
        return response(asset, variant, PUBLIC_CACHE);
    }

    @GetMapping("/access/{id}")
    ResponseEntity<Resource> protectedImage(@PathVariable Long id, @RequestParam String token, @RequestParam(required = false) String variant) {
        JwtService.MediaClaims claims;
        try { claims = jwt.parseMediaToken(token); }
        catch (Exception e) { throw DomainException.forbidden("图片访问凭证无效或已过期"); }
        if (!id.equals(claims.mediaId())) throw DomainException.forbidden("图片访问凭证无效");
        authorization.requireAccess(claims.userId(), id);
        return response(media.find(id), variant, PRIVATE_CACHE);
    }

    /** Streams from storage instead of buffering the whole image in memory. */
    private ResponseEntity<Resource> response(MediaAsset asset, String variant, CacheControl cache) {
        boolean thumbnail = "thumb".equals(variant) && asset.getThumbnailKey() != null;
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok().cacheControl(cache)
                .contentType(MediaType.parseMediaType(thumbnail ? MediaService.THUMBNAIL_CONTENT_TYPE : asset.getContentType()));
        if (!thumbnail) builder.contentLength(asset.getSizeBytes());
        return builder.body(new InputStreamResource(media.open(asset, thumbnail)));
    }
}
