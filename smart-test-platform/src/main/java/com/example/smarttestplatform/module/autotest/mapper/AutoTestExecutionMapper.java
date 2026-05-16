package com.example.smarttestplatform.module.autotest.mapper;

import com.example.smarttestplatform.module.autotest.entity.AutoTestExecution;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface AutoTestExecutionMapper {

    @Insert("INSERT INTO auto_test_execution(case_id, executor_id, start_time, status, ip, screenshot_url) " +
            "VALUES(#{caseId}, #{executorId}, #{startTime}, #{status}, #{ip}, #{screenshotUrl})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AutoTestExecution execution);

    @Update("UPDATE auto_test_execution SET end_time=#{endTime}, status=#{status}, result=#{result}, screenshot_url=#{screenshotUrl} WHERE id=#{id}")
    int update(AutoTestExecution execution);

    @Select("SELECT * FROM auto_test_execution WHERE id = #{id}")
    AutoTestExecution findById(@Param("id") Integer id);

    List<AutoTestExecution> pageQuery(@Param("conditions") Map<String, Object> conditions,
                                      @Param("offset") int offset,
                                      @Param("size") int size);

    Long count(@Param("conditions") Map<String, Object> conditions);

    @Select("SELECT * FROM auto_test_execution WHERE case_id = #{caseId} ORDER BY start_time DESC LIMIT 1")
    AutoTestExecution findLatestByCaseId(@Param("caseId") Integer caseId);

    @Select("SELECT * FROM auto_test_execution WHERE case_id = #{caseId} AND status = 'failed' ORDER BY start_time DESC LIMIT 1")
    AutoTestExecution findLastFailedByCaseId(@Param("caseId") Integer caseId);





    @Select("<script>"
            + "SELECT ate.* FROM auto_test_execution ate "
            + "INNER JOIN auto_test_case atc ON ate.case_id = atc.id "
            + "WHERE atc.project_id = #{projectId} "
            + "ORDER BY ate.start_time DESC LIMIT #{limit}"
            + "</script>")
    List<AutoTestExecution> findRecentByProjectId(@Param("projectId") Integer projectId, @Param("limit") Integer limit);

    @Select("<script>"
            + "SELECT ate.* FROM auto_test_execution ate "
            + "INNER JOIN auto_test_case atc ON ate.case_id = atc.id "
            + "WHERE atc.project_id = #{projectId}"
            + "</script>")
    List<AutoTestExecution> findByProjectId(@Param("projectId") Integer projectId);

}