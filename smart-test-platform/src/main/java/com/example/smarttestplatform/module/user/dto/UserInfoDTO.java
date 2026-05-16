package com.example.smarttestplatform.module.user.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserInfoDTO {
    private Integer id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private List<String> roles; // 角色代码列表，如 ["ROLE_ADMIN", "ROLE_TESTER"]
}