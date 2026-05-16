package com.example.smarttestplatform.module.user.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Role {
    private Integer id;
    private String roleName;
    private String roleCode;
    private String description;
    private Date createTime;
}