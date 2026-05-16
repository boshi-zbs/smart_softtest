package com.example.smarttestplatform.module.testexecution.service;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.testexecution.entity.TestExecution;

import java.util.Map;

public interface TestExecutionService {
    void createExecution(TestExecution execution, Integer executorId);
    PageResult<TestExecution> pageQuery(PageRequest pageRequest);
    TestExecution getLatestExecution(Integer planId, Integer caseId);
    // 统计某个计划的执行情况
    Map<String, Integer> getStatisticsByPlan(Integer planId);
}