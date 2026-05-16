package com.example.smarttestplatform.module.user.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
    private Integer status; // 0-禁用 1-启用
    private Date createTime;
    private Date updateTime;
    private List<Role> roles; // 不映射数据库
}