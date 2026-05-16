package com.example.smarttestplatform.module.projectmodule.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ProjectModule {
    private Integer id;
    private Integer projectId;
    private String moduleName;
    private String description;
    private Date createTime;

    // 非数据库字段
    private String projectName;
}