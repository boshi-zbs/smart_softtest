package com.example.smarttestplatform.module.testplan.service;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.testplan.entity.TestPlan;

import java.util.List;

public interface TestPlanService {
    TestPlan findById(Integer id);
    PageResult<TestPlan> pageQuery(PageRequest pageRequest);
    void createTestPlan(TestPlan testPlan, Integer creatorId);
    void updateTestPlan(TestPlan testPlan);
    void deleteTestPlan(Integer id);
    void deleteBatch(List<Integer> ids);

    // 关联用例相关
    void addCaseToPlan(Integer planId, Integer caseId);
    void removeCaseFromPlan(Integer planId, Integer caseId);
    List<Integer> getCaseIdsByPlanId(Integer planId);


    TestPlan getPlanWithProgress(Integer planId);
    PageResult<TestPlan> pageQueryWithProgress(PageRequest pageRequest);
}