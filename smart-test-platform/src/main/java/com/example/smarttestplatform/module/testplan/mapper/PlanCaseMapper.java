package com.example.smarttestplatform.module.testplan.mapper;

import com.example.smarttestplatform.module.testplan.entity.PlanCase;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PlanCaseMapper {

    @Insert("INSERT INTO plan_case(plan_id, case_id) VALUES(#{planId}, #{caseId})")
    int insertPlanCase(@Param("planId") Integer planId, @Param("caseId") Integer caseId);

    @Delete("DELETE FROM plan_case WHERE plan_id = #{planId} AND case_id = #{caseId}")
    int deletePlanCase(@Param("planId") Integer planId, @Param("caseId") Integer caseId);

    @Delete("DELETE FROM plan_case WHERE plan_id = #{planId}")
    int deleteByPlanId(@Param("planId")Integer planId);

    @Select("SELECT case_id FROM plan_case WHERE plan_id = #{planId}")
    List<Integer> findCaseIdsByPlanId(@Param("planId") Integer planId);

    @Select("SELECT COUNT(*) FROM plan_case WHERE plan_id = #{planId}")
    int countByPlanId(@Param("planId") Integer planId);

    @Select("SELECT * FROM plan_case WHERE plan_id = #{planId} AND case_id = #{caseId}")
    PlanCase findByPlanAndCase(@Param("planId") Integer planId, @Param("caseId") Integer caseId);

    @Select("SELECT DISTINCT plan_id FROM plan_case WHERE plan_id IN (SELECT id FROM test_plan WHERE project_id = #{projectId})")
    List<Integer> findPlanIdsByProjectId(@Param("projectId") Integer projectId);

    @Select("SELECT plan_id FROM plan_case WHERE case_id = #{caseId}")
    List<Integer> findPlanIdsByCaseId(@Param("caseId") Integer caseId);

}