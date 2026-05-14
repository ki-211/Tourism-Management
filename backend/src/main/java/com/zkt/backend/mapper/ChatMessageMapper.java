package com.zkt.backend.mapper;

import com.zkt.backend.entity.ChatMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    @Insert("INSERT INTO chat_message (activity_id, user_id, content, create_time) VALUES (#{activityId}, #{userId}, #{content}, NOW())")
    void insert(ChatMessage message);

    @Select("SELECT m.*, u.nickname FROM chat_message m LEFT JOIN user u ON m.user_id = u.id WHERE m.activity_id = #{activityId} ORDER BY m.create_time ASC")
    List<ChatMessage> findByActivityId(@Param("activityId") Long activityId);
}
