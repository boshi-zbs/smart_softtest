package com.example.smarttestplatform.module.project.mapper;

import com.example.smarttestplatform.module.project.entity.ProjectMember;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProjectMemberMapper {


    @Select("SELECT * FROM project_member WHERE project_id = #{projectId}")
    List<ProjectMember> findByProjectId(@Param("projectId") Integer projectId);

    @Select("SELECT * FROM project_member WHERE project_id = #{projectId} AND user_id = #{userId}")
    ProjectMember findByProjectAndUser(@Param("projectId") Integer projectId, @Param("userId") Integer userId);

    @Insert("INSERT INTO project_member(project_id, user_id, role_in_project, join_time) VALUES(#{projectId}, #{userId}, #{roleInProject}, NOW())")
    int insert(ProjectMember projectMember);

    @Update("UPDATE project_member SET role_in_project = #{roleInProject} WHERE project_id = #{projectId} AND user_id = #{userId}")
    int update(ProjectMember member);

    @Delete("DELETE FROM project_member WHERE project_id = #{projectId} AND user_id = #{userId}")
    int deleteByProjectAndUser(@Param("projectId") Integer projectId, @Param("userId") Integer userId);

    @Delete("DELETE FROM project_member WHERE project_id = #{projectId}")
    int deleteByProjectId(@Param("projectId") Integer projectId);

    // 分页查询（按项目）
    List<ProjectMember> pageQuery(@Param("conditions") Map<String, Object> conditions,
                                  @Param("offset") int offset,
                                  @Param("size") int size);

    Long count(@Param("conditions") Map<String, Object> conditions);

    @Select("SELECT project_id FROM project_member WHERE user_id = #{userId}")
    List<Integer> findProjectIdsByUserId(@Param("userId") Integer userId);

    @Select("SELECT * FROM project_member WHERE project_id = #{projectId} AND role_in_project = #{role}")
    List<ProjectMember> findByProjectIdAndRole(@Param("projectId") Integer projectId, @Param("role") String role);
}