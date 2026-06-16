package com.example.smarttestplatform.module.apitester.mapper;

import com.example.smarttestplatform.module.apitester.entity.ApiTestExecution;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ApiTestExecutionMapper {

    @Insert("INSERT INTO api_test_execution(case_id, executor_id, request_url, request_method, request_headers, request_body, response_status, response_body, assert_result, assert_message, duration_ms) " +
            "VALUES(#{execution.caseId}, #{execution.executorId}, #{execution.requestUrl}, #{execution.requestMethod}, #{execution.requestHeaders}, #{execution.requestBody}, #{execution.responseStatus}, #{execution.responseBody}, #{execution.assertResult}, #{execution.assertMessage}, #{execution.durationMs})")
    @Options(useGeneratedKeys = true, keyProperty = "execution.id")
    int insert(@Param("execution") ApiTestExecution execution);

    @Select("SELECT * FROM api_test_execution WHERE case_id = #{caseId} ORDER BY execute_time DESC LIMIT #{limit}")
    List<ApiTestExecution> findRecentByCaseId(@Param("caseId") Integer caseId, @Param("limit") int limit);

    /**
     * 获取多个用例的最近一次执行记录（用于计算通过率）
     */
    @Select("<script>" +
            "SELECT ate.* FROM api_test_execution ate " +
            "INNER JOIN ( " +
            "   SELECT case_id, MAX(execute_time) as max_time " +
            "   FROM api_test_execution " +
            "   WHERE case_id IN " +
            "   <foreach collection='caseIds' item='cid' open='(' separator=',' close=')'>#{cid}</foreach> " +
            "   GROUP BY case_id " +
            ") latest ON ate.case_id = latest.case_id AND ate.execute_time = latest.max_time" +
            "</script>")
    List<ApiTestExecution> findLatestByCaseIds(@Param("caseIds") List<Integer> caseIds);

    /**
     * 按项目和日期范围查询执行记录（用于项目报告中的已执行计数，可复用现有方法，但补充时间过滤）
     */
    @Select("<script>" +
            "SELECT ate.* FROM api_test_execution ate " +
            "JOIN api_test_case tc ON ate.case_id = tc.id " +
            "JOIN api_info ai ON tc.api_id = ai.id " +
            "WHERE ai.project_id = #{projectId} " +
            "<if test='startDate != null'>AND ate.execute_time >= #{startDate}</if>" +
            "<if test='endDate != null'>AND ate.execute_time &lt;= #{endDate}</if>" +
            "</script>")
    List<ApiTestExecution> findByProjectIdAndTimeRange(@Param("projectId") Integer projectId,
                                                       @Param("startDate") String startDate,
                                                       @Param("endDate") String endDate);

    @Delete("<script>" +
            "DELETE FROM api_test_execution WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    void batchDelete(@Param("ids") List<Integer> ids);
}