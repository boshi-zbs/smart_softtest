package com.example.smarttestplatform.module.testexecution.controller;

import com.example.smarttestplatform.common.annotation.Log;
import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.service.FileStorageService;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.testexecution.entity.TestExecution;
import com.example.smarttestplatform.module.testexecution.mapper.TestExecutionAttachmentMapper;
import com.example.smarttestplatform.module.testexecution.mapper.TestExecutionMapper;
import com.example.smarttestplatform.module.testexecution.service.TestExecutionService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test-executions")
@PreAuthorize("hasAnyRole('ADMIN', 'TESTER')")
public class TestExecutionController {

    @Autowired
    private TestExecutionService testExecutionService;
    @Autowired
    private UserService userService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private TestExecutionMapper testExecutionMapper;

    @Autowired
    private TestExecutionAttachmentMapper attachmentMapper;
    @Log(module = "测试执行", operation = "执行用例", description = "执行测试用例（记录执行结果）")
    @PostMapping
    public Result<String> create(@RequestBody TestExecution execution,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) return Result.error("用户不存在");
        testExecutionService.createExecution(execution, user.getId());
        return Result.success("执行成功");
    }

    @GetMapping("/statistics/plan/{planId}")
    public Result<Map<String, Integer>> getStatisticsByPlan(@PathVariable Integer planId) {
        Map<String, Integer> stats = testExecutionService.getStatisticsByPlan(planId);
        return Result.success(stats);
    }

    @PostMapping("/upload-attachment")
    public Result<Map<String, String>> uploadAttachment(@RequestParam("file") MultipartFile file) {
        System.out.println("===== uploadAttachment called =====");
        try {
            String fileUrl = fileStorageService.store(file);
            Map<String, String> data = new HashMap<>();
            data.put("url", fileUrl);
            data.put("fileName", file.getOriginalFilename());
            data.put("fileSize", String.valueOf(file.getSize()));
            data.put("fileType", file.getContentType());
            return Result.success(data);
        } catch (IOException e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
    @GetMapping("/latest/{caseId}")
    public Result<TestExecution> getLatestByCaseId(@PathVariable Integer caseId) {
        TestExecution execution = testExecutionMapper.findLatestByCaseId(caseId);
        if (execution != null) {
            // 加载附件列表
            execution.setAttachments(attachmentMapper.findByExecutionId(execution.getId()));
        }
        return Result.success(execution);
    }
}