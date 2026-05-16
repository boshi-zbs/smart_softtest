package com.example.smarttestplatform.module.projectmodule.mapper;

import com.example.smarttestplatform.module.projectmodule.entity.ProjectModule;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProjectModuleMapper {

    @Select("SELECT * FROM project_module WHERE id = #{id}")
    ProjectModule findById(@Param("id") Integer id);

    @Select("SELECT * FROM project_module WHERE project_id = #{projectId} ORDER BY module_name")
    List<ProjectModule> findByProjectId(@Param("projectId") Integer projectId);

    @Insert("INSERT INTO project_module(project_id, module_name, description) VALUES(#{projectId}, #{moduleName}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProjectModule module);

    @Update("UPDATE project_module SET module_name = #{moduleName}, description = #{description} WHERE id = #{id}")
    int update(ProjectModule module);

    @Delete("DELETE FROM project_module WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    @Delete("DELETE FROM project_module WHERE project_id = #{projectId}")
    int deleteByProjectId(@Param("projectId") Integer projectId);
}