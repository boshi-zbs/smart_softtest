package com.example.smarttestplatform.module.apitester.mapper;

import com.example.smarttestplatform.module.apitester.entity.ApiModule;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ApiModuleMapper {
    @Select("SELECT * FROM api_module WHERE id = #{id}")
    ApiModule findById(Integer id);

    @Select("SELECT * FROM api_module WHERE project_id = #{projectId} ORDER BY module_name")
    List<ApiModule> findByProjectId(Integer projectId);

    @Insert("INSERT INTO api_module(project_id, module_name, parent_id, description) VALUES(#{projectId}, #{moduleName}, #{parentId}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ApiModule module);

    @Update("UPDATE api_module SET module_name=#{moduleName}, parent_id=#{parentId}, description=#{description} WHERE id=#{id}")
    int update(ApiModule module);

    @Delete("DELETE FROM api_module WHERE id = #{id}")
    int deleteById(Integer id);
}