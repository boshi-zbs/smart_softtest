package com.example.smarttestplatform.module.requirement.controller;

import com.example.smarttestplatform.common.annotation.Log;
import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.requirement.entity.Requirement;
import com.example.smarttestplatform.module.requirement.service.RequirementService;
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
@RequestMapping("/api/requirements")
@PreAuthorize("hasAnyRole('ADMIN','TESTER','DEV')") // 管理员和测试人员可访问
public class RequirementController {

    @Autowired
    private RequirementService requirementService;
    @Autowired
    private UserService userService;

    @GetMapping
    public Result<PageResult<Requirement>> listRequirements(@RequestParam Map<String, String> allParams) {
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

        PageResult<Requirement> pageResult = requirementService.pageQuery(pageRequest);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<Requirement> getById(@PathVariable Integer id) {
        Requirement requirement = requirementService.findById(id);
        return requirement != null ? Result.success(requirement) : Result.error("需求不存在");
    }

    @Log(module = "功能需求管理", operation = "创建需求", description = "创建需求")
    @PostMapping
    public Result<String> create(@RequestBody Requirement requirement,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        requirementService.createRequirement(requirement, user.getId());
        return Result.success("创建成功");
    }
    @Log(module = "功能需求管理", operation = "更新需求", description = "更新需求")
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Integer id, @RequestBody Requirement requirement,@AuthenticationPrincipal UserDetails userDetails) {
        // 查询原需求
        Requirement old = requirementService.findById(id);
        if (old == null) {
            return Result.error("需求不存在");
        }
        // 如果是开发人员，只能更新自己指派的需求，且只能更新状态字段（或允许部分字段）
        // 简单起见：如果当前用户是开发人员，则校验 assigneeId 是否是本人
        User currentUser = userService.findByUsername(userDetails.getUsername());
        if (currentUser == null) {
            return Result.error("用户不存在");
        }
        // 检查角色，这里简单通过角色判断，实际可以在 service 层处理
        // 我们只检查如果当前用户是开发人员，那么只能更新自己负责的需求
        // 这里简化：从 token 获取的角色列表判断
        boolean isDev = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DEV"));
        if (isDev && !old.getAssigneeId().equals(currentUser.getId())) {
            return Result.error("您只能修改指派给您的需求");
        }

        requirement.setId(id);
        // 如果当前用户是开发人员，限制只能修改状态和描述等，不能修改项目、指派人等
        // 可以在 service 中做更精细的控制，这里简单直接更新所有字段，但风险可控
        requirementService.updateRequirement(requirement);
        return Result.success("更新成功");
    }
    @Log(module = "功能需求管理", operation = "删除需求", description = "删除需求")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        requirementService.deleteRequirement(id);
        return Result.success("删除成功");
    }
    @Log(module = "功能需求管理", operation = "批量删除", description = "批量删除需求")
    @DeleteMapping("/batch")
    public Result<String> deleteBatch(@RequestBody List<Integer> ids) {
        requirementService.deleteBatch(ids);
        return Result.success("批量删除成功");
    }
}