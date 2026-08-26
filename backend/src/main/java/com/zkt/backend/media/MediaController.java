package com.zkt.backend.media;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/media")
public class MediaController {
    private final MediaService media;
    public MediaController(MediaService media) { this.media = media; }

    @GetMapping("/public/{folder}/{name}")
    ResponseEntity<byte[]> get(@PathVariable String folder, @PathVariable String name) {
        var object = media.load(folder + "/" + name);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(object.contentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic()).body(object.content());
    }
}
