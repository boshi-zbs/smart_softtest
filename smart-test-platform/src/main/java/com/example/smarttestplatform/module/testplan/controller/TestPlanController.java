package com.example.smarttestplatform.module.testplan.controller;

import com.example.smarttestplatform.common.annotation.Log;
import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.testcase.entity.TestCase;
import com.example.smarttestplatform.module.testcase.mapper.TestCaseMapper;
import com.example.smarttestplatform.module.testexecution.entity.TestExecution;
import com.example.smarttestplatform.module.testexecution.service.TestExecutionService;
import com.example.smarttestplatform.module.testplan.entity.TestPlan;
import com.example.smarttestplatform.module.testplan.service.TestPlanService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test-plans")
@PreAuthorize("hasAnyRole('ADMIN', 'TESTER')")
public class TestPlanController {

    @Autowired
    private TestPlanService testPlanService;
    @Autowired
    private UserService userService;
    @Autowired
    private TestCaseMapper testCaseMapper;

    @Autowired
    private TestExecutionService testExecutionService;

    // 分页查询（带进度）
    @GetMapping
    public Result<PageResult<TestPlan>> listTestPlans(@RequestParam Map<String, String> allParams) {
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

        // 修改为调用带进度的方法
        PageResult<TestPlan> pageResult = testPlanService.pageQueryWithProgress(pageRequest);
        return Result.success(pageResult);
    }

    // 获取单个计划（带进度）
    @GetMapping("/{id}")
    public Result<TestPlan> getById(@PathVariable Integer id) {
        // 改为调用 getPlanWithProgress，内部已包含进度和关联用例ID
        TestPlan plan = testPlanService.getPlanWithProgress(id);
        if (plan == null) {
            return Result.error("计划不存在");
        }
        return Result.success(plan);
    }

    // 创建计划
    @Log(module = "测试计划与进度", operation = "创建计划", description = "创建计划")
    @PostMapping
    public Result<String> create(@RequestBody TestPlan testPlan,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        testPlanService.createTestPlan(testPlan, user.getId());
        return Result.success("创建成功");
    }

    // 更新计划
    @Log(module = "测试计划与进度", operation = "更新计划", description = "更新计划")
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Integer id, @RequestBody TestPlan testPlan) {
        testPlan.setId(id);
        testPlanService.updateTestPlan(testPlan);
        return Result.success("更新成功");
    }

    // 删除计划
    @Log(module = "测试计划与进度", operation = "删除计划", description = "删除计划")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        testPlanService.deleteTestPlan(id);
        return Result.success("删除成功");
    }

    // 批量删除
    @Log(module = "测试计划与进度", operation = "批量删除", description = "批量删除测试计划")
    @DeleteMapping("/batch")
    public Result<String> deleteBatch(@RequestBody List<Integer> ids) {
        testPlanService.deleteBatch(ids);
        return Result.success("批量删除成功");
    }

    // 添加用例到计划
    @Log(module = "测试计划与进度", operation = "添加用例", description = "向计划添加用例")
    @PostMapping("/{planId}/cases/{caseId}")
    public Result<String> addCase(@PathVariable Integer planId, @PathVariable Integer caseId) {
        testPlanService.addCaseToPlan(planId, caseId);
        return Result.success("添加成功");
    }

    // 从计划移除用例
    @Log(module = "测试计划与进度", operation = "移除用例", description = "从计划移除用例")
    @DeleteMapping("/{planId}/cases/{caseId}")
    public Result<String> removeCase(@PathVariable Integer planId, @PathVariable Integer caseId) {
        testPlanService.removeCaseFromPlan(planId, caseId);
        return Result.success("移除成功");
    }

//     获取计划关联的用例ID列表
    @GetMapping("/{planId}/cases/ids")
    public Result<List<Integer>> getCaseIds(@PathVariable Integer planId) {
        List<Integer> caseIds = testPlanService.getCaseIdsByPlanId(planId);
        return Result.success(caseIds);
    }

//    返回用例详情
    @GetMapping("/{planId}/cases")
    public Result<List<TestCase>> getPlanCases(@PathVariable Integer planId) {
        List<Integer> caseIds = testPlanService.getCaseIdsByPlanId(planId);
        if (caseIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        List<TestCase> cases = testCaseMapper.findByIds(caseIds);
        // 为每个用例填充最新执行结果
        for (TestCase tc : cases) {
            TestExecution latest = testExecutionService.getLatestExecution(planId, tc.getId());
            if (latest != null) {
                tc.setLastResult(latest.getResult());
            }
        }
        return Result.success(cases);
    }
}