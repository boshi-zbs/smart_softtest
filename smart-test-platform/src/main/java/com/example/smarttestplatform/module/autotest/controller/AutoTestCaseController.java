package com.example.smarttestplatform.module.autotest.controller;

import com.example.smarttestplatform.common.annotation.Log;
import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.autotest.dto.ExecuteResult;
import com.example.smarttestplatform.module.autotest.entity.AutoTestCase;
import com.example.smarttestplatform.module.autotest.entity.AutoTestStep;
import com.example.smarttestplatform.module.autotest.entity.ExecuteRequest;
import com.example.smarttestplatform.module.autotest.service.AutoTestCaseService;
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
@RequestMapping("/api/auto-test-cases")
@PreAuthorize("hasAnyRole('ADMIN', 'TESTER')")
public class AutoTestCaseController {

    @Autowired
    private AutoTestCaseService testCaseService;
    @Autowired
    private UserService userService;

    @GetMapping
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
        return Result.success(testCaseService.pageQuery(pageRequest));
    }

    @GetMapping("/{id}")
    public Result<AutoTestCase> getById(@PathVariable Integer id) {
        AutoTestCase testCase = testCaseService.findById(id);
        return testCase != null ? Result.success(testCase) : Result.error("用例不存在");
    }

    @Log(module = "自动化测试", operation = "创建用例", description = "创建自动化测试用例")
    @PostMapping
    public Result<String> create(@RequestBody AutoTestCaseDto dto,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) return Result.error("用户不存在");
        testCaseService.createCase(dto.getTestCase(), dto.getSteps(), user.getId());
        return Result.success("创建成功");
    }
    @Log(module = "自动化测试", operation = "更新用例", description = "更新自动化测试用例")
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Integer id, @RequestBody AutoTestCaseDto dto) {
        dto.getTestCase().setId(id);
        testCaseService.updateCase(dto.getTestCase(), dto.getSteps());
        return Result.success("更新成功");
    }
    @Log(module = "自动化测试", operation = "删除用例", description = "删除自动化测试用例")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        testCaseService.deleteCase(id);
        return Result.success("删除成功");
    }
    @Log(module = "自动化测试", operation = "执行用例", description = "执行自动化测试用例")
    @PostMapping("/{id}/execute")
    public Result<Map<String, Object>> execute(@PathVariable Integer id,
                                               @AuthenticationPrincipal UserDetails userDetails,
                                               @RequestParam(required = false) String ip,
                                               @RequestBody(required = false) ExecuteRequest executeRequest) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) return Result.error("用户不存在");
        String clientIp = ip != null ? ip : "127.0.0.1";

        Boolean headless = null;
        if (executeRequest != null && executeRequest.getHeadless() != null) {
            headless = executeRequest.getHeadless();
        } else {
            AutoTestCase testCase = testCaseService.findById(id);
            if (testCase != null && testCase.getHeadless() != null) {
                headless = testCase.getHeadless() == 1;
            } else {
                headless = true;
            }
        }

        // 修改：返回执行记录ID
        Integer executionId = testCaseService.executeCaseAndReturnId(id, user.getId(), clientIp, headless);
        Map<String, Object> result = new HashMap<>();
        result.put("id", executionId);
        return Result.success(result);
    }
}

// DTO 用于接收前端传入的用例和步骤
class AutoTestCaseDto {
    private AutoTestCase testCase;
    private List<AutoTestStep> steps;
    // getters/setters
    public AutoTestCase getTestCase() { return testCase; }
    public void setTestCase(AutoTestCase testCase) { this.testCase = testCase; }
    public List<AutoTestStep> getSteps() { return steps; }
    public void setSteps(List<AutoTestStep> steps) { this.steps = steps; }
}