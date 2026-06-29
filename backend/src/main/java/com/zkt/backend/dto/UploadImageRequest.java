package com.zkt.backend.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传请求参数封装
 * 用于 POST /api/upload/image 接口的 multipart/form-data 表单绑定
 */
@Data
public class UploadImageRequest {
    /** 图片文件（表单字段名 file） */
    private MultipartFile file;
    /** 所属活动ID */
    private Long activityId;
    /** 上传用户ID */
    private Long userId;
}
