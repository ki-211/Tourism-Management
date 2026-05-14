package com.zkt.backend.service;

import com.zkt.backend.entity.AlbumPhoto;
import java.util.List;

public interface AlbumPhotoService {
    void add(AlbumPhoto photo);
    List<AlbumPhoto> listByActivity(Long activityId);
}
