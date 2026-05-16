package com.example.smarttestplatform.module.testexecution.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class TestExecution {
    private Integer id;
    private Integer planId;
    private Integer caseId;
    private Integer executorId;
    private Date executeTime;
    private String result;          // 通过、失败、阻塞、跳过
    private String actualResult;
    private Integer durationMs;
    private Boolean isAutomated;
    private Date createTime;

    // 非数据库字段，用于显示
    private String executorName;
    private String caseTitle;
    private String planName;
    // 非数据库字段，用于前端展示
    private List<TestExecutionAttachment> attachments;
}