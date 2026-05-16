package com.example.smarttestplatform.module.defect.mapper;

import com.example.smarttestplatform.module.defect.entity.Defect;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface DefectMapper {

    @Select("SELECT * FROM defect WHERE id = #{id}")
    Defect findById(@Param("id") Integer id);

    @Insert("INSERT INTO defect(title, description, project_id, test_case_id, requirement_id, " +
            "severity, priority, status, assignee_id, found_version, fixed_version, " +
            "auto_execution_id, reporter_id, create_time) " +
            "VALUES(#{title}, #{description}, #{projectId}, #{testCaseId}, #{requirementId}, " +
            "#{severity}, #{priority}, #{status}, #{assigneeId}, #{foundVersion}, #{fixedVersion}, " +
            "#{autoExecutionId}, #{reporterId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Defect defect);

    @Update("UPDATE defect SET title=#{title}, description=#{description}, project_id=#{projectId}, " +
            "test_case_id=#{testCaseId}, requirement_id=#{requirementId}, severity=#{severity}, " +
            "priority=#{priority}, status=#{status}, assignee_id=#{assigneeId}, " +
            "found_version=#{foundVersion}, fixed_version=#{fixedVersion} " +
            "WHERE id=#{id}")
    int update(Defect defect);

    @Delete("DELETE FROM defect WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    List<Defect> pageQuery(@Param("conditions") Map<String, Object> conditions,
                           @Param("offset") int offset,
                           @Param("size") int size);

    Long count(@Param("conditions") Map<String, Object> conditions);

    @Select("SELECT status, COUNT(*) as count FROM defect WHERE project_id = #{projectId} GROUP BY status")
    List<Map<String, Object>> countByStatus(@Param("projectId") Integer projectId);

    @Select("SELECT severity, COUNT(*) as count FROM defect WHERE project_id = #{projectId} GROUP BY severity")
    List<Map<String, Object>> countBySeverity(@Param("projectId") Integer projectId);

    @Select("SELECT priority, COUNT(*) as count FROM defect WHERE project_id = #{projectId} GROUP BY priority")
    List<Map<String, Object>> countByPriority(@Param("projectId") Integer projectId);

    @Select("SELECT COUNT(*) FROM defect WHERE project_id = #{projectId} AND status IN ('新建', '已指派', '修复中')")
    int countOpenByProjectId(@Param("projectId") Integer projectId);

    @Select("SELECT DATE(create_time) as date, COUNT(*) as count FROM defect WHERE project_id = #{projectId} GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> countByCreateDate(@Param("projectId") Integer projectId);

    int countByProjectIds(@Param("ids") List<Integer> ids);
    int countOpenByProjectIds(@Param("ids") List<Integer> ids);
    List<Map<String, Object>> findAssignedToUser(@Param("userId") Integer userId, @Param("limit") Integer limit);
    List<Defect> findRecentByProjectIds(@Param("projectIds") List<Integer> projectIds, @Param("limit") Integer limit);
    List<Map<String, Object>> countByCreateDateRange(@Param("projectIds") List<Integer> projectIds, @Param("days") Integer days);
    List<Map<String, Object>> countByProjectIdsAndStatus(@Param("projectIds") List<Integer> projectIds);

}