package com.example.smarttestplatform.module.project.controller;

import com.example.smarttestplatform.common.annotation.Log;
import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.project.entity.ProjectMember;
import com.example.smarttestplatform.module.project.service.ProjectMemberService;
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
@RequestMapping("/api/project-members")
@PreAuthorize("hasRole('ADMIN')")
public class ProjectMemberController {

    @Autowired
    private ProjectMemberService projectMemberService;
    @Autowired
    private UserService userService;

    @GetMapping
    public Result<PageResult<ProjectMember>> listMembers(@RequestParam Map<String, String> allParams) {
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

        PageResult<ProjectMember> pageResult = projectMemberService.pageQuery(pageRequest);
        return Result.success(pageResult);
    }

    @GetMapping("/project/{projectId}")
    public Result<List<ProjectMember>> listByProject(@PathVariable Integer projectId) {
        List<ProjectMember> members = projectMemberService.findByProjectId(projectId);
        return Result.success(members);
    }

    @Log(module = "成员管理", operation = "添加成员", description = "添加成员")
    @PostMapping
    public Result<String> addMember(@RequestBody ProjectMember projectMember,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        projectMemberService.addMember(projectMember, user.getId());
        return Result.success("添加成功");
    }
    @Log(module = "成员管理", operation = "更新成员", description = "更新成员")
    @PutMapping
    public Result<String> updateMember(@RequestBody ProjectMember projectMember) {
        // 前端传入的 projectMember 必须包含 projectId 和 userId 以及 roleInProject
        projectMemberService.updateMember(projectMember);
        return Result.success("更新成功");
    }
    @Log(module = "成员管理", operation = "删除成员", description = "删除成员")
    @DeleteMapping("/{projectId}/{userId}")
    public Result<String> removeMember(@PathVariable Integer projectId, @PathVariable Integer userId) {
        projectMemberService.removeMember(projectId, userId);
        return Result.success("移除成功");
    }
    @Log(module = "成员管理", operation = "批量移除", description = "批量移除项目成员")
    @DeleteMapping("/batch")
    public Result<String> removeBatch(@RequestBody List<ProjectMember> members) {
        projectMemberService.removeBatch(members);
        return Result.success("批量移除成功");
    }


}