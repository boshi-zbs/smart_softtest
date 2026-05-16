package com.example.smarttestplatform.module.user.mapper;

import com.example.smarttestplatform.module.user.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(@Param("id") Integer id);

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(@Param("username") String username);

    @Insert("INSERT INTO user(username, password, real_name, email, phone, status) " +
            "VALUES(#{username}, #{password}, #{realName}, #{email}, #{phone}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET password=#{password}, real_name=#{realName}, email=#{email}, " +
            "phone=#{phone}, status=#{status} WHERE id=#{id}")
    int update(User user);

    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    @Select("SELECT * FROM user")
    List<User> findAll();

    List<User> pageQuery(@Param("conditions") Map<String, Object> conditions,
                         @Param("offset") int offset,
                         @Param("size") int size);

    Long count(@Param("conditions") Map<String, Object> conditions);

    void deleteBatch(@Param("ids") List<Integer> ids);

}