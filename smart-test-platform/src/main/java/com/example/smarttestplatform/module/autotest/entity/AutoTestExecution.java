package com.example.smarttestplatform.module.autotest.entity;

import lombok.Data;
import java.util.Date;

@Data
public class AutoTestExecution {
    private Integer id;
    private Integer caseId;
    private Integer executorId;
    private Date startTime;
    private Date endTime;
    private String status;      // running, success, failed
    private String result;      // 执行日志
    private String ip;
    private String screenshotUrl;  // 新增字段
    // 非数据库字段
    private String caseName;
    private String executorName;
}