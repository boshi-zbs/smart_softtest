package com.example.smarttestplatform.module.defect.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class Defect {
    private Integer id;
    private String title;
    private String description;
    private String severity;      // 致命、严重、一般、轻微
    private String priority;      // 最高、高、中、低
    private String status;        // 新建、已指派、修复中、已修复、验证中、已关闭、重新打开、驳回
    private Integer reporterId;
    private Integer assigneeId;
    private Integer testCaseId;
    private Integer requirementId;
    private Integer projectId;
    private String foundVersion;
    private String fixedVersion;

    private Integer autoExecutionId;  // 关联的自动化执行ID
    private Date createTime;
    private Date updateTime;

    // 删除 private String attachmentUrl;
    private List<DefectAttachment> attachments;
    // 非数据库字段
    private String projectName;
    private String reporterName;
    private String assigneeName;
    private String testCaseTitle;
    private String requirementTitle;
}