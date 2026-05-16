package com.example.smarttestplatform.module.testcase.mapper;

import com.example.smarttestplatform.module.testcase.entity.TestCase;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface TestCaseMapper {

    @Select("SELECT * FROM test_case WHERE id = #{id}")
    TestCase findById(@Param("id") Integer id);

    @Insert("INSERT INTO test_case(requirement_id, project_id, module_id, title, precondition, steps, expected_result, priority, type, status, creator_id) " +
            "VALUES(#{requirementId}, #{projectId}, #{moduleId}, #{title}, #{precondition}, #{steps}, #{expectedResult}, #{priority}, #{type}, #{status}, #{creatorId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TestCase testCase);

    @Update("UPDATE test_case SET requirement_id=#{requirementId}, module_id=#{moduleId}, title=#{title}, precondition=#{precondition}, steps=#{steps}, " +
            "expected_result=#{expectedResult}, priority=#{priority}, type=#{type}, status=#{status} WHERE id=#{id}")
    int update(TestCase testCase);

    @Delete("DELETE FROM test_case WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    List<TestCase> pageQuery(@Param("conditions") Map<String, Object> conditions,
                             @Param("offset") int offset,
                             @Param("size") int size);

    Long count(@Param("conditions") Map<String, Object> conditions);

    // 批量查询用例详情（用于关联ID列表）
    List<TestCase> findByIds(@Param("ids") List<Integer> ids);



    @Select("SELECT COUNT(*) FROM test_case WHERE project_id = #{projectId}")
    int countByProjectIdDirect(@Param("projectId") Integer projectId);

    int countByProjectIds(@Param("ids") List<Integer> ids);

}