package com.example.smarttestplatform.module.autotest.service;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.autotest.entity.AutoTestCase;
import com.example.smarttestplatform.module.autotest.entity.AutoTestExecution;
import com.example.smarttestplatform.module.autotest.entity.AutoTestStep;

import java.util.List;

public interface AutoTestService {
    // 用例管理
    AutoTestCase findTestCaseById(Integer id);
    PageResult<AutoTestCase> pageQueryTestCase(PageRequest pageRequest);
    void createTestCase(AutoTestCase testCase, Integer creatorId, List<AutoTestStep> steps);
    void updateTestCase(AutoTestCase testCase, List<AutoTestStep> steps);
    void deleteTestCase(Integer id);
    void deleteBatch(List<Integer> ids);

    // 步骤管理
    List<AutoTestStep> getStepsByCaseId(Integer caseId);

    // 执行管理
    AutoTestExecution executeTestCase(Integer caseId, Integer executorId, String baseUrlOverride);
    PageResult<AutoTestExecution> pageQueryExecution(PageRequest pageRequest);

    AutoTestExecution findExecutionById(Integer id);

    List<Integer> batchExecuteTestCase(List<Integer> caseIds, Integer executorId);

    AutoTestExecution findLastFailedExecutionByCaseId(Integer caseId);
}