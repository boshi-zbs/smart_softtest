package com.example.smarttestplatform.module.testcase.controller;

import com.example.smarttestplatform.common.annotation.Log;
import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.testcase.dto.BatchCreateRequest;
import com.example.smarttestplatform.module.testcase.entity.TestCase;
import com.example.smarttestplatform.module.testcase.service.AITestCaseService;
import com.example.smarttestplatform.module.testcase.service.TestCaseService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test-cases")
@PreAuthorize("hasAnyRole('ADMIN', 'TESTER')")
public class TestCaseController {

    @Autowired
    private TestCaseService testCaseService;
    @Autowired
    private UserService userService;
    @Autowired
    private AITestCaseService aiTestCaseService;
    @GetMapping
    public Result<PageResult<TestCase>> listTestCases(@RequestParam Map<String, String> allParams) {
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
        PageResult<TestCase> pageResult = testCaseService.pageQuery(pageRequest);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<TestCase> getById(@PathVariable Integer id) {
        TestCase testCase = testCaseService.findById(id);
        return testCase != null ? Result.success(testCase) : Result.error("用例不存在");
    }
    @Log(module = "测试用例管理", operation = "创建用例", description = "创建测试用例")
    @PostMapping
    public Result<String> create(@RequestBody TestCase testCase,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        testCaseService.createTestCase(testCase, user.getId());
        return Result.success("创建成功");
    }
    @Log(module = "测试用例管理", operation = "更新用例", description = "更新测试用例")
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Integer id, @RequestBody TestCase testCase) {
        testCase.setId(id);
        testCaseService.updateTestCase(testCase);
        return Result.success("更新成功");
    }
    @Log(module = "测试用例管理", operation = "删除用例", description = "删除测试用例")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        testCaseService.deleteTestCase(id);
        return Result.success("删除成功");
    }
    @Log(module = "测试用例管理", operation = "批量删除", description = "批量删除测试用例")
    @DeleteMapping("/batch")
    public Result<String> deleteBatch(@RequestBody List<Integer> ids) {
        testCaseService.deleteBatch(ids);
        return Result.success("批量删除成功");
    }

    // 1. AI生成接口（使用 @RequestPart 接收混合参数）
    @PostMapping("/ai-generate")
    public Result<List<Map<String, Object>>> aiGenerate(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "projectId", required = false) Integer projectId) {
        List<Map<String, Object>> suggestions = aiTestCaseService.generateTestCases(file, content, projectId);
        return Result.success(suggestions);
    }

    // 2. 批量创建接口
    @PostMapping("/batch-create")
    public Result<String> batchCreate(@RequestBody BatchCreateRequest request,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) return Result.error("用户不存在");
        testCaseService.batchCreate(request.getCases(), request.getPlanId(), user.getId());
        return Result.success("批量创建成功");
    }
}