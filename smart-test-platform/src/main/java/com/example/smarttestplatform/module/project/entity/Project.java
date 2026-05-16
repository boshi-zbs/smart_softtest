package com.example.smarttestplatform.module.project.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Project {
    private Integer id;
    private String projectName;
    private String projectKey;
    private String description;
    private Date startDate;
    private Date endDate;
    private Integer status;      // 1-进行中，0-已归档
    private Integer createUserId;
    private Date createTime;
    private Date updateTime;

    // 非数据库字段，用于显示创建人姓名
    private String createUserName;
}