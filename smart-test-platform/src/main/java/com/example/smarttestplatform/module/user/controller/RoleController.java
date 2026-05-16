package com.example.smarttestplatform.module.user.controller;

import com.example.smarttestplatform.common.annotation.Log;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.user.entity.Role;
import com.example.smarttestplatform.module.user.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    // 公开接口：获取所有角色（供注册页面使用）
    @GetMapping("/public/list")
    public Result<List<Role>> getPublicRoles() {
        return Result.success(roleService.findAll());
    }

    // 以下接口需要 ADMIN 权限
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<Role>> listAll() {
        return Result.success(roleService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Role> getById(@PathVariable Integer id) {
        return Result.success(roleService.findById(id));
    }

    @Log(module = "角色管理", operation = "创建角色", description = "创建新角色")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> create(@RequestBody Role role) {
        roleService.createRole(role);
        return Result.success("创建成功");
    }

    @Log(module = "角色管理", operation = "更新角色", description = "更新角色信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> update(@PathVariable Integer id, @RequestBody Role role) {
        role.setId(id);
        roleService.updateRole(role);
        return Result.success("更新成功");
    }
    @Log(module = "角色管理", operation = "删除角色", description = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> delete(@PathVariable Integer id) {
        roleService.deleteRole(id);
        return Result.success("删除成功");
    }
}