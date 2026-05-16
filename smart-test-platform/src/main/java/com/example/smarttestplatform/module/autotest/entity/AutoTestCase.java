package com.example.smarttestplatform.module.autotest.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class AutoTestCase {
    private Integer id;
    private Integer projectId;
    private String caseName;
    private String description;
    private String url;              // 对应数据库 url 列
    private Integer status;
    private Integer createUserId;     // 对应数据库 create_user_id
    private Date createTime;
    private Date updateTime;
    // 新增：运行模式（0-有头模式，1-无头模式）
    private Integer headless;  // 添加这个字段

    // 非数据库字段
    private String projectName;
    private String creatorName;       // 用于显示创建人姓名
    private List<AutoTestStep> steps;
    // 新增字段：最近一次执行状态和开始时间（非数据库字段）
    private String latestStatus;      // 最近执行状态：success/failed/running
    private Date latestStartTime;     // 最近执行时间（可选）
}