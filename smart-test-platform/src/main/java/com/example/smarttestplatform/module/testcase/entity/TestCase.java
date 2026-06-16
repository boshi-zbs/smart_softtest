package com.example.smarttestplatform.module.testcase.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class TestCase {
    private Integer id;
    private Integer requirementId;
    private String title;
    private String precondition;
    private String steps;
    private String expectedResult;
    private Integer priority;              // 1-最高，2-高，3-中，4-低
    private String type;                   // 功能测试、性能测试等
    private String status;                  // 有效、无效、废弃
    private Integer creatorId;
    private Date createTime;
    private Date updateTime;

    // 非数据库字段，用于显示
    private String requirementTitle;
    private String creatorName;
    private String lastResult;              // 最近一次执行结果（通过/失败等）
    private List<String> planNames;         // 关联的测试计划名称列表
    private Integer planId;                  // 用于创建时接收计划ID
    private String planName;                 // 用于显示当前计划名称（编辑时）

    private Integer moduleId;          // 新增
    private String moduleName;         // 非数据库字段，用于显示
    private Integer projectId;  // 项目ID
}