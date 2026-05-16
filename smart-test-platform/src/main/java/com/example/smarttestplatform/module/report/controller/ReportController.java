package com.example.smarttestplatform.module.report.controller;

import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.report.dto.AutoTestStats;
import com.example.smarttestplatform.module.report.dto.DashboardStats;
import com.example.smarttestplatform.module.report.dto.ProjectReportStats;
import com.example.smarttestplatform.module.report.service.ReportService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'TESTER')")
public class ReportController {

    @Autowired
    private ReportService reportService;
    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    public Result<DashboardStats> getDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) {
            return Result.error("用户不存在");
        }
        DashboardStats stats = reportService.getDashboardStats(user.getId());
        return Result.success(stats);
    }

    @GetMapping("/project/{projectId}")
    public Result<ProjectReportStats> getProjectReport(@PathVariable Integer projectId) {
        ProjectReportStats stats = reportService.getProjectReportStats(projectId);
        return Result.success(stats);
    }

    @GetMapping("/project/{projectId}/detail")
    public Result<ProjectReportStats> getProjectReportWithDateRange(
            @PathVariable Integer projectId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        ProjectReportStats stats = reportService.getProjectReportStatsWithDateRange(projectId, startDate, endDate);
        return Result.success(stats);
    }

    @GetMapping("/project/{projectId}/auto-test")
    public Result<AutoTestStats> getAutoTestStats(@PathVariable Integer projectId) {
        AutoTestStats stats = reportService.getAutoTestStats(projectId);
        return Result.success(stats);
    }
}