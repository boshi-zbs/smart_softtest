package com.example.smarttestplatform.module.report.dto;

import lombok.Data;

@Data
public class ProjectReportStats {
    private String projectName;
    private String projectKey;
    private Integer totalRequirements;      // 总需求数
    private Integer coveredRequirements;    // 已覆盖需求（有关联用例的需求数）
    private Double requirementCoverage;     // 覆盖率
    private ExecutionStats executionStats;  // 测试执行统计
    private DefectStats defectStats;        // 缺陷统计
    private AutoTestStats autoTestStats;
}