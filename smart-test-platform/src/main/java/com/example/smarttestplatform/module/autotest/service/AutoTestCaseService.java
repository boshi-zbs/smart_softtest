package com.example.smarttestplatform.module.autotest.service;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.autotest.dto.ExecuteResult;
import com.example.smarttestplatform.module.autotest.entity.AutoTestCase;
import com.example.smarttestplatform.module.autotest.entity.AutoTestStep;

import java.util.List;

public interface AutoTestCaseService {
    AutoTestCase findById(Integer id);
    PageResult<AutoTestCase> pageQuery(PageRequest pageRequest);
    void createCase(AutoTestCase testCase, List<AutoTestStep> steps, Integer createUserId);
    void updateCase(AutoTestCase testCase, List<AutoTestStep> steps);
    void deleteCase(Integer id);
    ExecuteResult executeCase(Integer caseId, Integer executorId, String ip, Boolean headless);
    // 新增：执行用例并返回执行记录ID
    Integer executeCaseAndReturnId(Integer caseId, Integer executorId, String ip, Boolean headless);
}