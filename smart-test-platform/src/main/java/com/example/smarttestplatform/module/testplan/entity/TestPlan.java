package com.example.smarttestplatform.module.testplan.entity;

import com.example.smarttestplatform.module.testcase.entity.TestCase;
import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class TestPlan {
    private Integer id;
    private Integer projectId;
    private String planName;
    private String description;
    private Date startDate;
    private Date endDate;
    private String status;          // 未开始、进行中、已完成、已取消
    private Integer creatorId;
    private Date createTime;
    private Date updateTime;


    // 非数据库字段，用于显示关联信息
    private String projectName;
    private String creatorName;
    private List<Integer> caseIds;   // 计划关联的用例ID列表（用于表单提交）
    private List<TestCase> cases;    // 计划关联的用例详情

    private Integer totalCases;      // 总用例数
    private Integer executedCases;   // 已执行用例数
    private Integer passedCases;     // 通过用例数
    private Integer failedCases;     // 失败用例数
    private Double progress;         // 进度百分比
}