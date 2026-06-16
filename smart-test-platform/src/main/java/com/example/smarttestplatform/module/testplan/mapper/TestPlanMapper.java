package com.example.smarttestplatform.module.testplan.mapper;

import com.example.smarttestplatform.module.testplan.entity.TestPlan;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface TestPlanMapper {

    @Select("SELECT * FROM test_plan WHERE id = #{id}")
    TestPlan findById(@Param("id") Integer id);

    @Insert("INSERT INTO test_plan(project_id, plan_name, description, start_date, end_date, status, creator_id, assignee_id) " +
            "VALUES(#{projectId}, #{planName}, #{description}, #{startDate}, #{endDate}, #{status}, #{creatorId}, #{assigneeId})")
    int insert(TestPlan testPlan);

    @Update("UPDATE test_plan SET project_id=#{projectId}, plan_name=#{planName}, description=#{description}, " +
            "start_date=#{startDate}, end_date=#{endDate}, status=#{status}, assignee_id=#{assigneeId} WHERE id=#{id}")
    int update(TestPlan testPlan);

    @Delete("DELETE FROM test_plan WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    List<TestPlan> pageQuery(@Param("conditions") Map<String, Object> conditions,
                             @Param("offset") int offset,
                             @Param("size") int size);

    Long count(@Param("conditions") Map<String, Object> conditions);

    @Select("SELECT case_id FROM plan_case WHERE plan_id = #{planId}")
    List<Integer> findCaseIdsByPlanId(@Param("id") Integer planId);
}