package com.example.smarttestplatform.module.autotest.controller;

import com.example.smarttestplatform.common.annotation.Log;
import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.autotest.entity.*;
import com.example.smarttestplatform.module.autotest.service.AutoTestService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auto-test")
@PreAuthorize("hasAnyRole('ADMIN', 'TESTER')")
public class AutoTestController {

    @Autowired
    private AutoTestService autoTestService;
    @Autowired
    private UserService userService;

    // ========== 用例管理 ==========
    @GetMapping("/cases")
    public Result<PageResult<AutoTestCase>> listCases(@RequestParam Map<String, String> allParams) {
        PageRequest pageRequest = new PageRequest();
        String pageStr = allParams.getOrDefault("page", "1");
        String sizeStr = allParams.getOrDefault("size", "10");
        try {
            pageRequest.setPage(Integer.parseInt(pageStr));
            pageRequest.setSize(Integer.parseInt(sizeStr));
        } catch (NumberFormatException e) {
            pageRequest.setPage(1);
            pageRequest.setSize(10);
        }
        allParams.remove("page");
        allParams.remove("size");
        pageRequest.setConditions(new HashMap<>(allParams));
        return Result.success(autoTestService.pageQueryTestCase(pageRequest));
    }

    @GetMapping("/cases/{id}")
    public Result<AutoTestCase> getCaseById(@PathVariable Integer id) {
        AutoTestCase testCase = autoTestService.findTestCaseById(id);
        return testCase != null ? Result.success(testCase) : Result.error("用例不存在");
    }

    @Log(module = "自动化测试", operation = "创建用例", description = "创建自动化测试用例")
    @PostMapping("/cases")
    public Result<String> createCase(@RequestBody AutoTestCaseRequest request,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) return Result.error("用户不存在");
        autoTestService.createTestCase(request.getTestCase(), user.getId(), request.getSteps());
        return Result.success("创建成功");
    }
    @Log(module = "自动化测试", operation = "更新用例", description = "更新自动化测试用例")
    @PutMapping("/cases/{id}")
    public Result<String> updateCase(@PathVariable Integer id, @RequestBody AutoTestCaseRequest request) {
        request.getTestCase().setId(id);
        autoTestService.updateTestCase(request.getTestCase(), request.getSteps());
        return Result.success("更新成功");
    }
    @Log(module = "自动化测试", operation = "删除用例", description = "删除自动化测试用例")
    @DeleteMapping("/cases/{id}")
    public Result<String> deleteCase(@PathVariable Integer id) {
        autoTestService.deleteTestCase(id);
        return Result.success("删除成功");
    }
    @Log(module = "自动化测试", operation = "批量删除", description = "批量删除自动化测试用例")
    @DeleteMapping("/cases/batch")
    public Result<String> deleteCases(@RequestBody List<Integer> ids) {
        autoTestService.deleteBatch(ids);
        return Result.success("批量删除成功");
    }

    // ========== 步骤管理 ==========
    @GetMapping("/cases/{caseId}/steps")
    public Result<List<AutoTestStep>> getSteps(@PathVariable Integer caseId) {
        return Result.success(autoTestService.getStepsByCaseId(caseId));
    }

    // ========== 执行管理 ==========
    @Log(module = "自动化测试", operation = "执行用例", description = "执行自动化测试用例")
    @PostMapping("/cases/{caseId}/execute")
    public Result<AutoTestExecution> executeCase(@PathVariable Integer caseId,
                                                 @RequestBody(required = false) ExecuteRequest executeRequest,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) return Result.error("用户不存在");
        String baseUrlOverride = executeRequest != null ? executeRequest.getBaseUrl() : null;
        AutoTestExecution execution = autoTestService.executeTestCase(caseId, user.getId(), baseUrlOverride);
        return Result.success(execution);
    }

    @GetMapping("/executions")
    public Result<PageResult<AutoTestExecution>> listExecutions(@RequestParam Map<String, String> allParams) {
        PageRequest pageRequest = new PageRequest();
        String pageStr = allParams.getOrDefault("page", "1");
        String sizeStr = allParams.getOrDefault("size", "10");
        try {
            pageRequest.setPage(Integer.parseInt(pageStr));
            pageRequest.setSize(Integer.parseInt(sizeStr));
        } catch (NumberFormatException e) {
            pageRequest.setPage(1);
            pageRequest.setSize(10);
        }
        allParams.remove("page");
        allParams.remove("size");
        pageRequest.setConditions(new HashMap<>(allParams));
        return Result.success(autoTestService.pageQueryExecution(pageRequest));
    }

    @GetMapping("/executions/{id}")
    public Result<AutoTestExecution> getExecution(@PathVariable Integer id) {
        AutoTestExecution execution = autoTestService.findExecutionById(id);
        return execution != null ? Result.success(execution) : Result.error("执行记录不存在");
    }

    @Log(module = "自动化测试", operation = "批量执行", description = "批量执行自动化测试用例")
    @PostMapping("/cases/batch-execute")
    public Result<List<Integer>> batchExecute(@RequestBody List<Integer> caseIds,
                                              @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) return Result.error("用户不存在");
        List<Integer> executionIds = autoTestService.batchExecuteTestCase(caseIds, user.getId());
        return Result.success(executionIds);
    }

    @GetMapping("/cases/{caseId}/last-failed-execution")
    public Result<AutoTestExecution> getLastFailedExecution(@PathVariable Integer caseId) {
        AutoTestExecution execution = autoTestService.findLastFailedExecutionByCaseId(caseId);
        return execution != null ? Result.success(execution) : Result.error("没有失败的执行记录");
    }
}

