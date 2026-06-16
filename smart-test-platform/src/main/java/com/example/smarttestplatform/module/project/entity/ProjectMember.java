package com.example.smarttestplatform.module.project.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ProjectMember {
    private Integer projectId;
    private Integer userId;
    private String roleInProject;   // 项目内角色，如 PM、DEV、TEST
    private Date joinTime;

    // 非数据库字段，用于显示关联信息
    private String projectName;
    private String userName;
    private String userRealName;
}