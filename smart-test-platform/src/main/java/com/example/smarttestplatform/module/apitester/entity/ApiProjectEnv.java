package com.example.smarttestplatform.module.apitester.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ApiProjectEnv {
    private Integer id;
    private Integer projectId;
    private String envName;
    private Boolean isDefault;
    private String variables;  // JSON字符串，存储键值对
    private Date createTime;
    private Date updateTime;
}