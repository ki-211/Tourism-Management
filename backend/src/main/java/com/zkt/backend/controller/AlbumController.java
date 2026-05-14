package com.zkt.backend.controller;

import com.zkt.backend.common.Result;
import com.zkt.backend.entity.AlbumPhoto;
import com.zkt.backend.service.AlbumPhotoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/album")
public class AlbumController {

    @Autowired
    private AlbumPhotoService albumPhotoService;

    @PostMapping("/add")
    public Result<?> add(@RequestBody AlbumPhoto photo) {
        try {
            albumPhotoService.add(photo);
            return Result.success("上传成功");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    @GetMapping("/list/{activityId}")
    public Result<List<AlbumPhoto>> list(@PathVariable Long activityId) {
        try {
            List<AlbumPhoto> list = albumPhotoService.listByActivity(activityId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("获取图册失败：" + e.getMessage());
        }
    }
}
