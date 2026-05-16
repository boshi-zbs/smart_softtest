package com.example.smarttestplatform.module.project.controller;

import com.example.smarttestplatform.common.annotation.Log;
import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.project.entity.Project;
import com.example.smarttestplatform.module.project.service.ProjectService;
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
@RequestMapping("/api/projects")
@PreAuthorize("hasAnyRole('ADMIN', 'TESTER', 'DEV')")   // 修改这里，添加 DEV
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserService userService;

    @GetMapping
    public Result<PageResult<Project>> listProjects(@RequestParam Map<String, String> allParams) {
        PageRequest pageRequest = new PageRequest();

        // 处理分页参数
        String pageStr = allParams.getOrDefault("page", "1");
        String sizeStr = allParams.getOrDefault("size", "10");
        try {
            pageRequest.setPage(Integer.parseInt(pageStr));
            pageRequest.setSize(Integer.parseInt(sizeStr));
        } catch (NumberFormatException e) {
            pageRequest.setPage(1);
            pageRequest.setSize(10);
        }

        // 移除分页参数，剩余作为查询条件
        allParams.remove("page");
        allParams.remove("size");
        pageRequest.setConditions(new HashMap<>(allParams));

        PageResult<Project> pageResult = projectService.pageQuery(pageRequest);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<Project> getById(@PathVariable Integer id) {
        Project project = projectService.findById(id);
        return project != null ? Result.success(project) : Result.error("项目不存在");
    }

    @Log(module = "项目管理", operation = "创建项目", description = "创建项目")
    @PostMapping
    public Result<String> create(@RequestBody Project project,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        projectService.createProject(project, user.getId());
        return Result.success("创建成功");
    }
    @Log(module = "项目管理", operation = "更新项目", description = "更新项目")
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Integer id, @RequestBody Project project) {
        project.setId(id);
        projectService.updateProject(project);
        return Result.success("更新成功");
    }
    @Log(module = "项目管理", operation = "删除项目", description = "删除项目")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        projectService.deleteProject(id);
        return Result.success("删除成功");
    }
    @Log(module = "项目管理", operation = "批量删除", description = "批量删除项目")
    @DeleteMapping("/batch")
    public Result<String> deleteBatch(@RequestBody List<Integer> ids) {
        projectService.deleteBatch(ids);
        return Result.success("批量删除成功");
    }
}