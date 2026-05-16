// OperationLogController.java
package com.example.smarttestplatform.module.operationlog.controller;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.operationlog.entity.OperationLog;
import com.example.smarttestplatform.module.operationlog.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operation-logs")
@PreAuthorize("hasRole('ADMIN')")
public class OperationLogController {

    @Autowired
    private OperationLogService operationLogService;

    @GetMapping
    public Result<PageResult<OperationLog>> listLogs(@RequestParam Map<String, String> allParams) {
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
        PageResult<OperationLog> pageResult = operationLogService.pageQuery(pageRequest);
        return Result.success(pageResult);
    }

    // 获取所有模块选项
    @GetMapping("/modules")
    public Result<List<String>> getModules() {
        List<String> modules = operationLogService.getAllModules();
        return Result.success(modules);
    }

    // 获取所有操作类型选项
    @GetMapping("/operations")
    public Result<List<String>> getOperations() {
        List<String> operations = operationLogService.getAllOperations();
        return Result.success(operations);
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> batchDelete(@RequestBody List<Integer> ids) {
        operationLogService.batchDelete(ids);
        return Result.success("删除成功");
    }

    @DeleteMapping("/clear-all")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> clearAll() {
        operationLogService.clearAll();
        return Result.success("清空成功");
    }
}