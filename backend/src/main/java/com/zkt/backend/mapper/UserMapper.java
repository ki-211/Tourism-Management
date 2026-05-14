package com.zkt.backend.mapper;

import com.zkt.backend.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM `user` WHERE username = #{username}")
    User findByUsername(String username);

    @Insert("INSERT INTO `user`(username, password, nickname, role) " +
            "VALUES(#{username}, #{password}, #{nickname}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT * FROM `user` WHERE id = #{userId}")
    User findByUserId(Long userId);

    @Update("UPDATE user SET nickname = #{nickname}, updated_at = NOW() WHERE id = #{id}")
    int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);
}
