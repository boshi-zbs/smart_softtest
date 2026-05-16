package com.example.smarttestplatform.module.projectmodule.controller;

import com.example.smarttestplatform.common.annotation.Log;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.projectmodule.entity.ProjectModule;
import com.example.smarttestplatform.module.projectmodule.service.ProjectModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project-modules")
@PreAuthorize("hasAnyRole('ADMIN', 'TESTER')")   // 改为 ADMIN 或 TESTER
public class ProjectModuleController {

    @Autowired
    private ProjectModuleService projectModuleService;

    @GetMapping("/project/{projectId}")
    public Result<List<ProjectModule>> listByProject(@PathVariable Integer projectId) {
        List<ProjectModule> list = projectModuleService.findByProjectId(projectId);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<ProjectModule> getById(@PathVariable Integer id) {
        ProjectModule module = projectModuleService.findById(id);
        return module != null ? Result.success(module) : Result.error("模块不存在");
    }
    @Log(module = "模块管理", operation = "创建模块", description = "创建项目模块")
    @PostMapping
    public Result<String> create(@RequestBody ProjectModule module) {
        projectModuleService.createModule(module);
        return Result.success("创建成功");
    }
    @Log(module = "模块管理", operation = "更新模块", description = "更新模块信息")
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Integer id, @RequestBody ProjectModule module) {
        module.setId(id);
        projectModuleService.updateModule(module);
        return Result.success("更新成功");
    }
    @Log(module = "模块管理", operation = "删除模块", description = "删除项目模块")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        projectModuleService.deleteModule(id);
        return Result.success("删除成功");
    }
}