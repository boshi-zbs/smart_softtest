package com.example.smarttestplatform.module.apitester.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ApiInfo {
    private Integer id;
    private Integer projectId;
    private Integer moduleId;
    private String name;
    private String method;
    private String url;
    private String headers;  // JSON
    private String description;
    private Integer createUserId;
    private Date createTime;
    private Date updateTime;

    // 非DB
    private String moduleName;
    private String projectName;
}