package com.example.smarttestplatform.module.project.mapper;

import com.example.smarttestplatform.module.project.entity.Project;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProjectMapper {

    @Select("SELECT * FROM project WHERE id = #{id}")
    Project findById(@Param("id") Integer id);

    @Select("SELECT * FROM project WHERE project_key = #{projectKey}")
    Project findByKey(@Param("projectKey") String projectKey);

    @Insert("INSERT INTO project(project_name, project_key, description, start_date, end_date, status, create_user_id) " +
            "VALUES(#{projectName}, #{projectKey}, #{description}, #{startDate}, #{endDate}, #{status}, #{createUserId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Project project);

    @Update("UPDATE project SET project_name=#{projectName}, project_key=#{projectKey}, description=#{description}, " +
            "start_date=#{startDate}, end_date=#{endDate}, status=#{status} WHERE id=#{id}")
    int update(Project project);

    @Delete("DELETE FROM project WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    List<Project> pageQuery(@Param("conditions") Map<String, Object> conditions,
                            @Param("offset") int offset,
                            @Param("size") int size);

    Long count(@Param("conditions") Map<String, Object> conditions);


    @Select("SELECT id FROM project ORDER BY id")
    List<Integer> findAllProjectIds();

}