package com.example.smarttestplatform.module.requirement.mapper;

import com.example.smarttestplatform.module.requirement.entity.Requirement;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface RequirementMapper {

    @Select("SELECT * FROM requirement WHERE id = #{id}")
    Requirement findById(@Param("id") Integer id);

    @Insert("INSERT INTO requirement(project_id, title, description, priority, status, assignee_id, creator_id) " +
            "VALUES(#{projectId}, #{title}, #{description}, #{priority}, #{status}, #{assigneeId}, #{creatorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Requirement requirement);

    @Update("UPDATE requirement SET project_id=#{projectId}, title=#{title}, description=#{description}, " +
            "priority=#{priority}, status=#{status}, assignee_id=#{assigneeId} WHERE id=#{id}")
    int update(Requirement requirement);

    @Delete("DELETE FROM requirement WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    List<Requirement> pageQuery(@Param("conditions") Map<String, Object> conditions,
                                @Param("offset") int offset,
                                @Param("size") int size);

    Long count(@Param("conditions") Map<String, Object> conditions);



    @Select("SELECT COUNT(*) FROM requirement WHERE project_id = #{projectId}")
    int countByProjectId(@Param("projectId") Integer projectId);

    @Select("SELECT COUNT(DISTINCT r.id) FROM requirement r INNER JOIN test_case t ON r.id = t.requirement_id WHERE r.project_id = #{projectId}")
    int countCoveredByProjectId(@Param("projectId") Integer projectId);

    int countByProjectIds(@Param("ids") List<Integer> ids);


}