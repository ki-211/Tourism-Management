package com.zkt.backend.media;

import com.zkt.backend.auth.JwtService;
import com.zkt.backend.common.DomainException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {
    private final MediaService media;
    private final MediaAuthorizationService authorization;
    private final JwtService jwt;

    public MediaController(MediaService media, MediaAuthorizationService authorization, JwtService jwt) {
        this.media = media; this.authorization = authorization; this.jwt = jwt;
    }

    @GetMapping("/public/{id}")
    ResponseEntity<byte[]> publicCover(@PathVariable Long id) {
        MediaAsset asset = media.find(id);
        if (!"COVER".equals(asset.getPurpose())) throw DomainException.forbidden("该图片不是公开封面");
        return response(media.load(id), true);
    }

    @GetMapping("/access/{id}")
    ResponseEntity<byte[]> protectedImage(@PathVariable Long id, @RequestParam String token) {
        JwtService.MediaClaims claims;
        try { claims = jwt.parseMediaToken(token); }
        catch (Exception e) { throw DomainException.forbidden("图片访问凭证无效或已过期"); }
        if (!id.equals(claims.mediaId())) throw DomainException.forbidden("图片访问凭证无效");
        authorization.requireAccess(claims.userId(), id);
        return response(media.load(id), false);
    }

    private ResponseEntity<byte[]> response(ObjectStorage.StoredObject object, boolean publicCache) {
        CacheControl cache = publicCache ? CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic()
                : CacheControl.noStore().cachePrivate();
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(object.contentType()))
                .cacheControl(cache).body(object.content());
    }
}
