package com.example.smarttestplatform.module.user.mapper;

import com.example.smarttestplatform.module.user.entity.Role;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RoleMapper {

    @Select("SELECT * FROM role WHERE id = #{id}")
    Role findById(@Param("id") Integer id);

    @Select("SELECT * FROM role WHERE role_code = #{roleCode}")
    Role findByCode(@Param("roleCode") String roleCode);

    @Select("SELECT r.* FROM role r JOIN user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<Role> findRolesByUserId(@Param("userId") Integer userId);

    @Insert("INSERT INTO role(role_name, role_code, description) VALUES(#{roleName}, #{roleCode}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Role role);

    @Update("UPDATE role SET role_name=#{roleName}, role_code=#{roleCode}, description=#{description} WHERE id=#{id}")
    int update(Role role);

    @Delete("DELETE FROM role WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    @Select("SELECT * FROM role")
    List<Role> findAll();
}