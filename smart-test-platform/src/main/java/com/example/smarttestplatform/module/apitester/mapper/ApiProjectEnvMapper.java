package com.example.smarttestplatform.module.apitester.mapper;

import com.example.smarttestplatform.module.apitester.entity.ApiProjectEnv;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ApiProjectEnvMapper {
    @Select("SELECT * FROM api_project_env WHERE project_id = #{projectId} AND is_default = 1")
    ApiProjectEnv findByProjectId(@Param("projectId") Integer projectId);

    @Insert("INSERT INTO api_project_env(project_id, env_name, is_default, variables) VALUES(#{projectId}, #{envName}, #{isDefault}, #{variables})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ApiProjectEnv env);

    @Update("UPDATE api_project_env SET variables = #{variables} WHERE id = #{id}")
    int update(ApiProjectEnv env);
}