package com.example.smarttestplatform.module.user.controller;

import com.example.smarttestplatform.common.annotation.Log;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.service.RoleService;
import com.example.smarttestplatform.module.user.service.UserService;
import com.example.smarttestplatform.common.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;

//    @Log(module = "认证管理", operation = "登录", description = "用户登录（记录用户名、IP、结果）")
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // 获取用户的角色列表
            List<String> userRoles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            Integer selectedRoleId = loginRequest.getRoleId();
            if (selectedRoleId != null) {
                String selectedRoleCode = roleService.findById(selectedRoleId).getRoleCode();
                if (!userRoles.contains(selectedRoleCode)) {
                    return Result.error("您没有该角色的访问权限");
                }
            } else {
                return Result.error("请选择登录角色");
            }

            String token = jwtUtils.generateToken(userDetails);
            Result<String> result = new Result<>();
            result.setCode(200);
            result.setMessage("success");
            result.setData(token);
            return result;
        } catch (InternalAuthenticationServiceException e) {
            // 解包，检查是否是 DisabledException
            if (e.getCause() instanceof DisabledException) {
                return Result.error("账号已禁用，请联系管理员");
            } else {
                e.printStackTrace();
                return Result.error("认证服务异常，请稍后重试");
            }
        } catch (AuthenticationException e) {
            return Result.error("用户名或密码错误");
        }
    }

//    @Log(module = "认证管理", operation = "注册", description = "用户注册（记录用户名）")
    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterRequest registerRequest) {
        // 检查用户名是否已存在
        if (userService.findByUsername(registerRequest.getUsername()) != null) {
            return Result.error("用户名已存在");
        }
        // 创建用户对象
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setRealName(registerRequest.getRealName());
        user.setEmail(registerRequest.getEmail());
        user.setPhone(registerRequest.getPhone());
        user.setStatus(1); // 默认启用

        // 分配角色（传入角色ID列表）
        List<Integer> roleIds = List.of(registerRequest.getRoleId());
        userService.createUser(user, roleIds);

        return Result.success("注册成功");
    }
}

// 登录请求体（增加 roleId）
class LoginRequest {
    private String username;
    private String password;
    private Integer roleId;  // 新增角色ID

    // getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }
}

// 注册请求体（增加 roleId）
class RegisterRequest {
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
    private Integer roleId;  // 新增角色ID

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
    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }
}