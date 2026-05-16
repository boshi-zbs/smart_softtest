package com.example.smarttestplatform.module.report.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class DashboardStats {
    private Integer totalProjects;
    private Integer totalRequirements;
    private Integer totalTestCases;
    private Integer totalDefects;
    private Integer openDefects;


    // 新增字段 - 项目列表及关键指标
    private List<ProjectSummary> projectSummaries;


    // 新增字段 - 最近活动
    private List<RecentActivity> recentActivities;

    // 新增字段 - 趋势数据（近 7 天）
    private DashboardTrend trend;

    // 新增字段 - 按状态分布的缺陷
    private Map<String, Integer> defectDistribution;

    @Data
    public static class ProjectSummary {
        private Integer projectId;
        private String projectName;
        private String projectKey;
        private Integer totalCases;
        private Integer executedCases;
        private Double passRate;
        private Integer activeDefects;
    }



    @Data
    public static class RecentActivity {
        private Integer id;
        private String activityType;  // 创建用例、执行测试、提交缺陷等
        private String description;
        private String operatorName;
        private Date createTime;
    }

    @Data
    public static class DashboardTrend {
        private List<DailyStats> dailyStats;  // 近 7 天统计数据
        private Integer weekExecutedCases;    // 本周执行用例数
        private Integer weekPassedCases;      // 本周通过用例数
        private Integer weekNewDefects;       // 本周新增缺陷
    }

    @Data
    public static class DailyStats {
        private String date;
        private Integer executedCases;
        private Integer passedCases;
        private Integer newDefects;
    }
}
