package com.example.smarttestplatform.module.apitester.mapper;

import com.example.smarttestplatform.module.apitester.entity.ApiTestCase;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ApiTestCaseMapper {
    @Select("SELECT * FROM api_test_case WHERE id = #{id}")
    ApiTestCase findById(Integer id);

    @Select("SELECT * FROM api_test_case WHERE api_id = #{apiId} ORDER BY create_time DESC")
    List<ApiTestCase> findByApiId(Integer apiId);

    @Insert("INSERT INTO api_test_case(api_id, case_name, request_params, expected_status, expected_response, assert_type, description, is_generated, create_user_id) VALUES(#{apiId}, #{caseName}, #{requestParams}, #{expectedStatus}, #{expectedResponse}, #{assertType}, #{description}, #{isGenerated}, #{createUserId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ApiTestCase testCase);

    @Update("UPDATE api_test_case SET case_name=#{caseName}, request_params=#{requestParams}, expected_status=#{expectedStatus}, expected_response=#{expectedResponse}, assert_type=#{assertType}, description=#{description}, is_generated=#{isGenerated} WHERE id=#{id}")
    int update(ApiTestCase testCase);

    @Delete("DELETE FROM api_test_case WHERE id = #{id}")
    int deleteById(Integer id);

    // 在接口中添加
    @Select("SELECT tc.*, " +
            "       (SELECT CASE WHEN assert_result=1 THEN '成功' ELSE '失败' END FROM api_test_execution WHERE case_id = tc.id ORDER BY execute_time DESC LIMIT 1) as lastExecutionResult, " +
            "       (SELECT execute_time FROM api_test_execution WHERE case_id = tc.id ORDER BY execute_time DESC LIMIT 1) as lastExecutionTime " +
            "FROM api_test_case tc WHERE api_id = #{apiId} ORDER BY tc.create_time DESC")
    List<ApiTestCase> findByApiIdWithLastResult(@Param("apiId") Integer apiId);

    /**
     * 统计某个项目下的接口测试用例总数
     */
    @Select("SELECT COUNT(*) FROM api_test_case WHERE api_id IN (SELECT id FROM api_info WHERE project_id = #{projectId})")
    Integer countByProjectId(@Param("projectId") Integer projectId);

    /**
     * 统计多个项目下的接口测试用例总数
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM api_test_case WHERE api_id IN " +
            "(SELECT id FROM api_info WHERE project_id IN " +
            "<foreach collection='projectIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>)" +
            "</script>")
    Integer countByProjectIds(@Param("projectIds") List<Integer> projectIds);

    /**
     * 获取某个项目下所有接口测试用例的 ID 列表（用于关联执行记录）
     */
    @Select("SELECT id FROM api_test_case WHERE api_id IN (SELECT id FROM api_info WHERE project_id = #{projectId})")
    List<Integer> findIdsByProjectId(@Param("projectId") Integer projectId);
}