package com.example.smarttestplatform.module.testexecution.mapper;

import com.example.smarttestplatform.module.testexecution.entity.TestExecution;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface TestExecutionMapper {

    @Select("SELECT * FROM test_execution WHERE id = #{id}")
    TestExecution findById(@Param("id") Integer id);

    @Insert("INSERT INTO test_execution(plan_id, case_id, executor_id, execute_time, result, actual_result, duration_ms) " +
            "VALUES(#{planId}, #{caseId}, #{executorId}, #{executeTime}, #{result}, #{actualResult}, #{durationMs})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TestExecution execution);

    @Update("UPDATE test_execution SET result=#{result}, actual_result=#{actualResult}, duration_ms=#{durationMs} WHERE id=#{id}")
    int update(TestExecution execution);

    @Delete("DELETE FROM test_execution WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    List<TestExecution> pageQuery(@Param("conditions") Map<String, Object> conditions,
                                  @Param("offset") int offset,
                                  @Param("size") int size);

    Long count(@Param("conditions") Map<String, Object> conditions);

    @Select("SELECT * FROM test_execution WHERE plan_id = #{planId} AND case_id = #{caseId} ORDER BY execute_time DESC LIMIT 1")
    TestExecution findLatestByPlanAndCase(@Param("planId") Integer planId, @Param("caseId") Integer caseId);


    @Select("SELECT te.* FROM test_execution te " +
            "INNER JOIN test_plan tp ON te.plan_id = tp.id " +
            "WHERE tp.project_id = #{projectId}")
    List<TestExecution> findByProjectId(@Param("projectId") Integer projectId);

    @Select("SELECT * FROM test_execution WHERE case_id = #{caseId} ORDER BY execute_time DESC LIMIT 1")
    TestExecution findLatestByCaseId(@Param("caseId") Integer caseId);




// ... existing code ...

    @Select("SELECT result, COUNT(*) as count FROM test_execution WHERE plan_id = #{planId} GROUP BY result")
    List<Map<String, Object>> countByPlanId(@Param("planId")Integer planId);


    Map<String, Object> countByPlanIds(@Param("planIds") List<Integer> planIds);
    List<TestExecution> findRecentByProjectIds(@Param("projectIds") List<Integer> projectIds, @Param("limit") Integer limit);
    List<Map<String, Object>> countByDateRange(@Param("projectIds") List<Integer> projectIds, @Param("days") Integer days);

}

