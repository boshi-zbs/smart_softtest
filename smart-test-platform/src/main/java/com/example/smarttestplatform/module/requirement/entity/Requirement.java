package com.example.smarttestplatform.module.requirement.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Requirement {
    private Integer id;
    private Integer projectId;
    private String title;
    private String description;
    private Integer priority;       // 1-最高，2-高，3-中，4-低
    private String status;          // 待处理、处理中、已完成、已关闭
    private Integer assigneeId;     // 负责人ID
    private Integer creatorId;      // 创建人ID
    private Date createTime;
    private Date updateTime;

    // 非数据库字段，用于显示关联信息
    private String projectName;
    private String assigneeName;    // 负责人姓名
    private String creatorName;     // 创建人姓名
}