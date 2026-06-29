package com.zkt.backend.mapper;

import com.zkt.backend.entity.AlbumPhoto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlbumPhotoMapper {
    int insert(AlbumPhoto photo);
    List<AlbumPhoto> selectByActivityId(@Param("activityId") Long activityId);
}
