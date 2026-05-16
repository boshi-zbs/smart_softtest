package com.example.smarttestplatform.module.user.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface UserRoleMapper {

    @Insert("INSERT INTO user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int insertUserRole(@Param("userId") Integer userId, @Param("roleId") Integer roleId);

    @Delete("DELETE FROM user_role WHERE user_id = #{userId} AND role_id = #{roleId}")
    int deleteUserRole(@Param("userId") Integer userId, @Param("roleId") Integer roleId);

    @Delete("DELETE FROM user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Integer userId);
}