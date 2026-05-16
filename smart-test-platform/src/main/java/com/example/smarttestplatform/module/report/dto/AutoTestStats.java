package com.example.smarttestplatform.module.report.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class AutoTestStats {
    private Integer totalAutoCases;           // 自动化用例总数
    private Integer executedCases;            // 已执行用例数
    private Integer passedCases;              // 通过用例数
    private Integer failedCases;              // 失败用例数
    private Double passRate;                  // 通过率
    private Integer totalExecutions;          // 总执行次数
    private Map<String, Integer> statusDistribution;  // 状态分布
    private List<ExecutionRecord> recentExecutions;   // 最近执行记录

    @Data
    public static class ExecutionRecord {
        private Integer executionId;
        private Integer caseId;
        private String caseName;
        private String status;
        private Date startTime;
        private String executorName;
    }
}
