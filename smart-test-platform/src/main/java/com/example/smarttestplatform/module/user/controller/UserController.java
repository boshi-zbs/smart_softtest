package com.example.smarttestplatform.module.user.controller;

import com.example.smarttestplatform.common.annotation.Log;
import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.user.dto.PasswordChangeDTO;
import com.example.smarttestplatform.module.user.dto.UpdateUserInfoDTO;
import com.example.smarttestplatform.module.user.dto.UserInfoDTO;
import com.example.smarttestplatform.module.user.entity.Role;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.service.RoleService;
import com.example.smarttestplatform.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

//    @GetMapping
//    public Result<List<User>> listAll() {
//        return Result.success(userService.findAll());
//    }

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Integer id) {
        User user = userService.findById(id);
        return user != null ? Result.success(user) : Result.error("用户不存在");
    }

    @Log(module = "用户管理", operation = "新增用户", description = "创建新用户")
    @PostMapping
    public Result<String> create(@RequestBody UserDto userDto) {
        if (userService.findByUsername(userDto.getUsername()) != null) {
            return Result.error("用户名已存在");
        }
        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setPassword(userDto.getPassword());
        user.setRealName(userDto.getRealName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        user.setStatus(userDto.getStatus());
        // 构造角色ID列表
        List<Integer> roleIds = new ArrayList<>();
        if (userDto.getRoleId() != null) {
            roleIds.add(userDto.getRoleId());
        }
        userService.createUser(user, roleIds);
        return Result.success("创建成功");
    }

    @Log(module = "用户管理", operation = "修改用户", description = "更新用户信息")
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Integer id, @RequestBody UserDto userDto) {
        User existingUser = userService.findByUsername(userDto.getUsername());
        if (existingUser != null && !existingUser.getId().equals(id)) {
            return Result.error("用户名已存在");
        }
        User user = new User();
        user.setId(id);
        user.setUsername(userDto.getUsername());  // 允许修改用户名
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            user.setPassword(userDto.getPassword());
        }
        user.setRealName(userDto.getRealName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        user.setStatus(userDto.getStatus());
        // 构造角色ID列表
        List<Integer> roleIds = new ArrayList<>();
        if (userDto.getRoleId() != null) {
            roleIds.add(userDto.getRoleId());
        }
        userService.updateUser(user, roleIds);
        return Result.success("更新成功");
    }

    @Log(module = "用户管理", operation = "删除用户", description = "删除用户")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        userService.deleteUser(id);
        return Result.success("删除成功");
    }

    @GetMapping("/{id}/roles")
    public Result<List<Role>> getUserRoles(@PathVariable Integer id) {
        return Result.success(userService.getUserRoles(id));
    }

    @Autowired
    private RoleService roleService;

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public Result<UserInfoDTO> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return Result.error("未登录");
        }
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        List<Role> roles = roleService.findRolesByUserId(user.getId());
        List<String> roleCodes = roles.stream().map(Role::getRoleCode).collect(Collectors.toList());

        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setRoles(roleCodes);
        return Result.success(dto);
    }

    @GetMapping("/profile")
    public Result<UserInfoDTO> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return Result.error("未登录");
        }
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        // 角色列表可以从 userDetails 获取，但我们已经有了 roles，可调用 roleService 或从 userDetails 获取
        // 简单起见，可以从 userDetails 的 authorities 获取
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        dto.setRoles(roles);
        return Result.success(dto);
    }

    // 更新当前用户信息（个人资料修改）
//    @Log(module = "用户管理", operation = "修改资料", description = "修改个人资料")
    @PutMapping("/profile")
    public Result<String> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                        @RequestBody UpdateUserInfoDTO updateDto) {
        if (userDetails == null) {
            return Result.error("未登录");
        }
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 更新允许修改的字段
        user.setRealName(updateDto.getRealName());
        user.setEmail(updateDto.getEmail());
        user.setPhone(updateDto.getPhone());
        // 注意：密码修改应单独接口，这里暂不处理

        userService.updateUser(user, null); // 不修改角色
        return Result.success("更新成功");
    }
//    @Log(module = "用户管理", operation = "修改密码", description = "修改登录密码")
    @PutMapping("/password")
    public Result<String> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                         @RequestBody PasswordChangeDTO dto) {
        if (userDetails == null) {
            return Result.error("未登录");
        }
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        userService.changePassword(user.getId(), dto.getOldPassword(), dto.getNewPassword());
        return Result.success("密码修改成功，请重新登录");
    }

    @GetMapping
    public Result<PageResult<User>> listUsers(@RequestParam Map<String, String> allParams) {
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
        // 将查询条件放入 conditions
        pageRequest.setConditions(new HashMap<>(allParams));

        PageResult<User> pageResult = userService.pageQuery(pageRequest);
        return Result.success(pageResult);
    }
    @Log(module = "用户管理", operation = "批量删除", description = "批量删除用户")
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> deleteBatch(@RequestBody List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的用户");
        }
        userService.deleteBatch(ids);
        return Result.success("批量删除成功");
    }
}

// DTO 类，必须有 getter/setter
class UserDto {
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
    private Integer status;
    private Integer roleId;

    // getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

}