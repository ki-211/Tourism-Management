package com.zkt.backend.controller;

import com.zkt.backend.common.Result;
import com.zkt.backend.dto.UploadImageRequest;
import com.zkt.backend.entity.AlbumPhoto;
import com.zkt.backend.service.AlbumPhotoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${upload.path:./uploads/}")
    private String uploadPath;

    @Value("${upload.url-prefix:http://localhost:8080/uploads/}")
    private String urlPrefix;

    @Resource
    private AlbumPhotoService albumPhotoService;

    /**
     * 根据活动ID上传图片：保存文件并将记录写入 album_photo 表
     *
     * @param request 上传请求参数（包含 file、activityId、userId）
     * @return 图片可访问 URL
     */
    @PostMapping("/image")
    public Result<String> uploadImage(UploadImageRequest request) {

        MultipartFile file = request.getFile();
        Long activityId = request.getActivityId();
        Long userId = request.getUserId();

        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        if (activityId == null || userId == null) {
            return Result.error("activityId 和 userId 不能为空");
        }

        // 校验文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.error("文件名异常");
        }
        String ext = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase()
                : "";
        if (!ext.matches("\\.(jpg|jpeg|png|gif|webp|bmp)")) {
            return Result.error("仅支持 jpg/jpeg/png/gif/webp/bmp 格式的图片");
        }

        // 生成唯一文件名，保存到磁盘
        String newFilename = UUID.randomUUID().toString().replace("-", "") + ext;
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File dest = new File(dir, newFilename).getAbsoluteFile();
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            return Result.error("文件保存失败：" + e.getMessage());
        }

        // 将图片记录写入 album_photo 表
        String url = urlPrefix + newFilename;
        AlbumPhoto photo = new AlbumPhoto();
        photo.setActivityId(activityId);
        photo.setUserId(userId);
        photo.setUrl(url);
        try {
            albumPhotoService.add(photo);
        } catch (RuntimeException e) {
            // 删除已保存的文件，保持一致性
            dest.delete();
            return Result.error(e.getMessage());
        } catch (Exception e) {
            dest.delete();
            return Result.error("保存图片记录失败：" + e.getMessage());
        }

        return Result.success(url);
    }

    /**
     * 根据活动ID获取图片列表
     *
     * @param activityId 活动ID
     * @return 该活动下所有图片记录
     */
    @GetMapping("/list/{activityId}")
    public Result<List<AlbumPhoto>> listByActivity(@PathVariable Long activityId) {
        try {
            List<AlbumPhoto> list = albumPhotoService.listByActivity(activityId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error("获取图片列表失败：" + e.getMessage());
        }
    }
}
