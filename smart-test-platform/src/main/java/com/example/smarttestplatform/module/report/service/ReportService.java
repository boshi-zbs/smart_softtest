package com.example.smarttestplatform.module.report.service;

import com.example.smarttestplatform.module.report.dto.AutoTestStats;
import com.example.smarttestplatform.module.report.dto.DashboardStats;
import com.example.smarttestplatform.module.report.dto.ProjectReportStats;

public interface ReportService {

    DashboardStats getDashboardStats(Integer userId);

    ProjectReportStats getProjectReportStats(Integer projectId);

    // 新增方法 - 带时间范围的项目报告
    ProjectReportStats getProjectReportStatsWithDateRange(Integer projectId, String startDate, String endDate);

    // 新增方法 - 获取自动化测试统计
    AutoTestStats getAutoTestStats(Integer projectId);
}
