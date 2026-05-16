package com.example.smarttestplatform.module.user.dto;

import lombok.Data;
import java.util.List;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
    private List<Integer> roleIds; // 选择的角色ID列表
}