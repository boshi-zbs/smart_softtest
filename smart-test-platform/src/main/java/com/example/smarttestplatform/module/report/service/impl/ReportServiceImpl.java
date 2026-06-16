package com.example.smarttestplatform.module.report.service.impl;

import com.example.smarttestplatform.module.apitester.entity.ApiTestExecution;
import com.example.smarttestplatform.module.apitester.mapper.ApiTestCaseMapper;
import com.example.smarttestplatform.module.apitester.mapper.ApiTestExecutionMapper;
import com.example.smarttestplatform.module.defect.entity.Defect;
import com.example.smarttestplatform.module.project.mapper.ProjectMapper;
import com.example.smarttestplatform.module.project.mapper.ProjectMemberMapper;
import com.example.smarttestplatform.module.requirement.mapper.RequirementMapper;
import com.example.smarttestplatform.module.testcase.mapper.TestCaseMapper;
import com.example.smarttestplatform.module.defect.mapper.DefectMapper;
import com.example.smarttestplatform.module.testexecution.entity.TestExecution;
import com.example.smarttestplatform.module.testexecution.mapper.TestExecutionMapper;
import com.example.smarttestplatform.module.testplan.mapper.PlanCaseMapper;
import com.example.smarttestplatform.module.report.dto.*;
import com.example.smarttestplatform.module.report.service.ReportService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.mapper.UserMapper;
import com.example.smarttestplatform.module.autotest.mapper.AutoTestCaseMapper;
import com.example.smarttestplatform.module.autotest.mapper.AutoTestExecutionMapper;
import com.example.smarttestplatform.module.autotest.entity.AutoTestCase;
import com.example.smarttestplatform.module.autotest.entity.AutoTestExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private ProjectMemberMapper projectMemberMapper;
    @Autowired
    private RequirementMapper requirementMapper;
    @Autowired
    private TestCaseMapper testCaseMapper;
    @Autowired
    private DefectMapper defectMapper;
    @Autowired
    private TestExecutionMapper testExecutionMapper;
    @Autowired
    private PlanCaseMapper planCaseMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AutoTestCaseMapper autoTestCaseMapper;
    @Autowired
    private AutoTestExecutionMapper autoTestExecutionMapper;

    @Autowired
    private ApiTestCaseMapper apiTestCaseMapper;
    @Autowired
    private ApiTestExecutionMapper apiTestExecutionMapper;
    @Override
    public DashboardStats getDashboardStats(Integer userId) {
        System.out.println("\n========== 获取仪表盘数据 ==========");
        System.out.println("📌 userId: " + userId);

        List<Integer> projectIds = getProjectIdsByUser(userId);
        System.out.println("📂 用户参与的项目 IDs: " + projectIds);

        if (projectIds.isEmpty()) {
            System.out.println("⚠️  警告：用户没有关联任何项目！");
            DashboardStats emptyStats = new DashboardStats();
            emptyStats.setTotalProjects(0);
            emptyStats.setTotalRequirements(0);
            emptyStats.setTotalTestCases(0);
            emptyStats.setTotalDefects(0);
            emptyStats.setOpenDefects(0);
            emptyStats.setProjectSummaries(new ArrayList<>());
            emptyStats.setRecentActivities(new ArrayList<>());
            emptyStats.setTrend(new DashboardStats.DashboardTrend());
            emptyStats.setDefectDistribution(new HashMap<>());
            return emptyStats;
        }

        System.out.println("📊 开始统计数据...");

        Integer totalProjects = projectIds.size();
        Integer totalRequirements = requirementMapper.countByProjectIds(projectIds);
        Integer totalTestCases = testCaseMapper.countByProjectIds(projectIds);
        Integer totalDefects = defectMapper.countByProjectIds(projectIds);
        Integer openDefects = defectMapper.countOpenByProjectIds(projectIds);

        // 接口测试统计
        Integer totalApiCases = apiTestCaseMapper.countByProjectIds(projectIds);
        DashboardStats stats = new DashboardStats();
        stats.setTotalProjects(totalProjects);
        stats.setTotalRequirements(totalRequirements);
        stats.setTotalTestCases(totalTestCases);
        stats.setTotalDefects(totalDefects);
        stats.setOpenDefects(openDefects);

        stats.setTotalApiCases(totalApiCases);
        // 计算通过率：基于所有用例的最新执行记录，计算成功比例
        double apiPassRate = 0.0;
        if (totalApiCases != null && totalApiCases > 0) {
            // 获取所有项目下的所有接口测试用例ID
            List<Integer> allApiCaseIds = new ArrayList<>();
            for (Integer pid : projectIds) {
                List<Integer> ids = apiTestCaseMapper.findIdsByProjectId(pid);
                if (ids != null) allApiCaseIds.addAll(ids);
            }
            if (!allApiCaseIds.isEmpty()) {
                List<ApiTestExecution> latestExecutions = apiTestExecutionMapper.findLatestByCaseIds(allApiCaseIds);
                long passedCount = latestExecutions.stream().filter(ApiTestExecution::getAssertResult).count();
                apiPassRate = passedCount * 100.0 / allApiCaseIds.size();
            }
        }
        stats.setApiPassRate(apiPassRate);

        stats.setProjectSummaries(buildProjectSummaries(projectIds));
        stats.setRecentActivities(buildRecentActivities(projectIds));
        stats.setTrend(buildDashboardTrend(projectIds));
        stats.setDefectDistribution(buildDefectDistribution(projectIds));

        System.out.println("✅ 项目摘要数量：" + stats.getProjectSummaries().size());
        System.out.println("✅ 最近活动数量：" + stats.getRecentActivities().size());
        System.out.println("====================================\n");

        return stats;
    }

    @Override
    public ProjectReportStats getProjectReportStats(Integer projectId) {
        return getProjectReportStatsWithDateRange(projectId, null, null);
    }

    @Override
    public ProjectReportStats getProjectReportStatsWithDateRange(Integer projectId, String startDate, String endDate) {
        ProjectReportStats stats = new ProjectReportStats();

        var project = projectMapper.findById(projectId);
        if (project == null) {
            return stats;
        }
        stats.setProjectName(project.getProjectName());
        stats.setProjectKey(project.getProjectKey());

        Integer totalRequirements = requirementMapper.countByProjectId(projectId);
        stats.setTotalRequirements(totalRequirements);

        Integer coveredRequirements = requirementMapper.countCoveredByProjectId(projectId);
        stats.setCoveredRequirements(coveredRequirements);
        stats.setRequirementCoverage(totalRequirements == 0 ? 0.0 : coveredRequirements * 100.0 / totalRequirements);

        stats.setExecutionStats(buildExecutionStats(projectId, startDate, endDate));
        stats.setDefectStats(buildDefectStats(projectId, startDate, endDate));

        // ========== 新增：集成自动化测试统计 ==========
        AutoTestStats autoStats = getAutoTestStats(projectId);
        stats.setAutoTestStats(autoStats);
        // =========================================
        stats.setApiTestStats(getApiTestStats(projectId, startDate, endDate));
        return stats;
    }

    @Override
    public AutoTestStats getAutoTestStats(Integer projectId) {
        AutoTestStats stats = new AutoTestStats();

        // 统计自动化用例
        Integer totalAutoCases = autoTestCaseMapper.countByProjectId(projectId);
        stats.setTotalAutoCases(totalAutoCases);

        // 统计执行情况
        List<AutoTestExecution> executions = autoTestExecutionMapper.findByProjectId(projectId);

        int executed = 0, passed = 0, failed = 0;
        Map<String, Integer> statusMap = new HashMap<>();

        for (AutoTestExecution execution : executions) {
            executed++;
            String status = execution.getStatus();
            statusMap.put(status, statusMap.getOrDefault(status, 0) + 1);

            if ("success".equals(status)) {
                passed++;
            } else if ("failed".equals(status)) {
                failed++;
            }
        }

        stats.setExecutedCases(executed);
        stats.setPassedCases(passed);
        stats.setFailedCases(failed);
        stats.setPassRate(executed > 0 ? (double) passed / executed * 100 : 0.0);
        stats.setTotalExecutions(executions.size());
        stats.setStatusDistribution(statusMap);

        // 最近执行记录
        stats.setRecentExecutions(buildRecentExecutions(projectId));

        return stats;
    }

    private List<Integer> getProjectIdsByUser(Integer userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            System.out.println("❌ 用户不存在，userId: " + userId);
            return Collections.emptyList();
        }

        System.out.println("✅ 当前用户：" + user.getUsername() + ", realName: " + user.getRealName());

        // 如果是管理员，返回所有项目 ID
        if ("admin".equals(user.getUsername())) {
            System.out.println("🔑 管理员用户，获取所有项目");
            List<Integer> allProjectIds = projectMapper.findAllProjectIds();
            System.out.println("📦 所有项目 IDs: " + allProjectIds);
            return allProjectIds != null ? allProjectIds : Collections.emptyList();
        }

        // 普通用户：根据成员关系获取项目
        List<Integer> ids = projectMemberMapper.findProjectIdsByUserId(userId);
        System.out.println("👤 普通用户，关联的项目 IDs: " + ids);
        return ids != null ? ids : Collections.emptyList();
    }

    private List<DashboardStats.ProjectSummary> buildProjectSummaries(List<Integer> projectIds) {
        return projectIds.stream().map(projectId -> {
            DashboardStats.ProjectSummary summary = new DashboardStats.ProjectSummary();
            var project = projectMapper.findById(projectId);
            if (project == null) return null;

            summary.setProjectId(projectId);
            summary.setProjectName(project.getProjectName());
            summary.setProjectKey(project.getProjectKey());

            // 统计用例 - 直接使用 projectId 查询
            Integer totalCases = testCaseMapper.countByProjectIdDirect(projectId);
            summary.setTotalCases(totalCases);

            // 统计执行情况 - 获取该项目下所有测试计划的执行总数
            List<Integer> planIds = planCaseMapper.findPlanIdsByProjectId(projectId);
            int executedCases = 0;
            int passedCases = 0;

            if (!planIds.isEmpty()) {
                Map<String, Object> executionCounts = testExecutionMapper.countByPlanIds(planIds);
                executedCases = ((Number) executionCounts.getOrDefault("total", 0)).intValue();
                passedCases = ((Number) executionCounts.getOrDefault("passed", 0)).intValue();
            }

            summary.setExecutedCases(executedCases);
            summary.setPassRate(executedCases > 0 ? (double) passedCases / executedCases * 100 : 0.0);

            // 活跃缺陷数 - 直接查询该项目的未关闭缺陷
            Integer activeDefects = defectMapper.countOpenByProjectId(projectId);
            summary.setActiveDefects(activeDefects);

            return summary;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<DashboardStats.RecentActivity> buildRecentActivities(List<Integer> projectIds) {
        List<DashboardStats.RecentActivity> activities = new ArrayList<>();

        // 最近的测试执行
        List<TestExecution> recentExecutions = testExecutionMapper.findRecentByProjectIds(projectIds, 10);
        for (TestExecution execution : recentExecutions) {
            DashboardStats.RecentActivity activity = new DashboardStats.RecentActivity();
            activity.setId(execution.getId());
            activity.setActivityType("测试执行");
            activity.setDescription("执行测试用例：" + execution.getCaseId());

            User executor = userMapper.findById(execution.getExecutorId());
            activity.setOperatorName(executor != null ? executor.getRealName() : "未知");
            activity.setCreateTime(execution.getExecuteTime());

            activities.add(activity);
        }

        // 最近的缺陷创建
        List<Defect> recentDefects = defectMapper.findRecentByProjectIds(projectIds, 5);
        for (Defect defect : recentDefects) {
            DashboardStats.RecentActivity activity = new DashboardStats.RecentActivity();
            activity.setId(defect.getId());
            activity.setActivityType("缺陷提交");
            activity.setDescription("提交缺陷：" + defect.getTitle());

            User reporter = userMapper.findById(defect.getReporterId());
            activity.setOperatorName(reporter != null ? reporter.getRealName() : "未知");
            activity.setCreateTime(defect.getCreateTime());

            activities.add(activity);
        }

        // 按时间排序，取最新的 10 条
        return activities.stream()
                .sorted(Comparator.comparing(DashboardStats.RecentActivity::getCreateTime).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    private DashboardStats.DashboardTrend buildDashboardTrend(List<Integer> projectIds) {
        DashboardStats.DashboardTrend trend = new DashboardStats.DashboardTrend();

        // 近 7 天的统计数据
        List<Map<String, Object>> dailyExecutions = testExecutionMapper.countByDateRange(projectIds, 7);
        List<Map<String, Object>> dailyDefects = defectMapper.countByCreateDateRange(projectIds, 7);

        Map<String, DashboardStats.DailyStats> dailyMap = new LinkedHashMap<>();

        // 初始化近 7 天
        Calendar calendar = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            calendar.add(Calendar.DAY_OF_MONTH, -i);
            String date = String.format("%tF", calendar);
            DashboardStats.DailyStats dailyStats = new DashboardStats.DailyStats();
            dailyStats.setDate(date);
            dailyStats.setExecutedCases(0);
            dailyStats.setPassedCases(0);
            dailyStats.setNewDefects(0);
            dailyMap.put(date, dailyStats);
        }

        // 填充执行数据
        for (Map<String, Object> record : dailyExecutions) {
            String date = record.get("date").toString();
            DashboardStats.DailyStats stats = dailyMap.get(date);
            if (stats != null) {
                int count = ((Long) record.get("count")).intValue();
                stats.setExecutedCases(stats.getExecutedCases() + count);
                if ("通过".equals(record.get("result"))) {
                    stats.setPassedCases(stats.getPassedCases() + count);
                }
            }
        }

        // 填充缺陷数据
        for (Map<String, Object> record : dailyDefects) {
            String date = record.get("date").toString();
            DashboardStats.DailyStats stats = dailyMap.get(date);
            if (stats != null) {
                int count = ((Long) record.get("count")).intValue();
                stats.setNewDefects(count);
            }
        }

        trend.setDailyStats(new ArrayList<>(dailyMap.values()));

        // 本周统计（简单实现，实际应该按自然周计算）
        trend.setWeekExecutedCases(dailyMap.values().stream().mapToInt(DashboardStats.DailyStats::getExecutedCases).sum());
        trend.setWeekPassedCases(dailyMap.values().stream().mapToInt(DashboardStats.DailyStats::getPassedCases).sum());
        trend.setWeekNewDefects(dailyMap.values().stream().mapToInt(DashboardStats.DailyStats::getNewDefects).sum());

        return trend;
    }

    private Map<String, Integer> buildDefectDistribution(List<Integer> projectIds) {
        List<Map<String, Object>> statusCount = defectMapper.countByProjectIdsAndStatus(projectIds);
        return statusCount.stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("status"),
                        m -> ((Long) m.get("count")).intValue()
                ));
    }

    private List<AutoTestStats.ExecutionRecord> buildRecentExecutions(Integer projectId) {
        List<AutoTestExecution> executions = autoTestExecutionMapper.findRecentByProjectId(projectId, 10);

        return executions.stream().map(execution -> {
            AutoTestStats.ExecutionRecord record = new AutoTestStats.ExecutionRecord();
            record.setExecutionId(execution.getId());
            record.setCaseId(execution.getCaseId());
            record.setStatus(execution.getStatus());
            record.setStartTime(execution.getStartTime());

            // 获取用例名称
            AutoTestCase testCase = autoTestCaseMapper.findById(execution.getCaseId());
            record.setCaseName(testCase != null ? testCase.getCaseName() : "未知");

            // 获取执行人名称
            User executor = userMapper.findById(execution.getExecutorId());
            record.setExecutorName(executor != null ? executor.getRealName() : "未知");

            return record;
        }).collect(Collectors.toList());
    }

    private ExecutionStats buildExecutionStats(Integer projectId, String startDate, String endDate) {
        List<Integer> planIds = planCaseMapper.findPlanIdsByProjectId(projectId);
        int totalCases = 0;
        for (Integer planId : planIds) {
            totalCases += planCaseMapper.countByPlanId(planId);
        }

        List<TestExecution> executions = testExecutionMapper.findByProjectId(projectId);

        // 如果有时间范围，进行过滤
        if (startDate != null && !startDate.isEmpty()) {
            Date start = parseDate(startDate);
            executions = executions.stream()
                    .filter(e -> e.getExecuteTime().compareTo(start) >= 0)
                    .collect(Collectors.toList());
        }
        if (endDate != null && !endDate.isEmpty()) {
            Date end = parseDate(endDate);
            executions = executions.stream()
                    .filter(e -> e.getExecuteTime().compareTo(end) <= 0)
                    .collect(Collectors.toList());
        }

        int executed = 0, passed = 0, failed = 0, blocked = 0, skipped = 0;
        for (TestExecution te : executions) {
            executed++;
            switch (te.getResult()) {
                case "通过": passed++; break;
                case "失败": failed++; break;
                case "阻塞": blocked++; break;
                case "跳过": skipped++; break;
            }
        }

        ExecutionStats execStats = new ExecutionStats();
        execStats.setTotalCases(totalCases);
        execStats.setExecutedCases(executed);
        execStats.setPassed(passed);
        execStats.setFailed(failed);
        execStats.setBlocked(blocked);
        execStats.setSkipped(skipped);
        execStats.setProgress(totalCases == 0 ? 0 : (double) executed / totalCases * 100);

        Map<String, ExecutionTrendItem> trendMap = new LinkedHashMap<>();
        for (TestExecution te : executions) {
            String date = te.getExecuteTime().toString().substring(0, 10);
            ExecutionTrendItem item = trendMap.computeIfAbsent(date, k -> {
                ExecutionTrendItem i = new ExecutionTrendItem();
                i.setDate(k);
                return i;
            });
            switch (te.getResult()) {
                case "通过": item.setPassed(item.getPassed() + 1); break;
                case "失败": item.setFailed(item.getFailed() + 1); break;
                case "阻塞": item.setBlocked(item.getBlocked() + 1); break;
            }
        }
        execStats.setTrend(new ArrayList<>(trendMap.values()));

        return execStats;
    }

    private DefectStats buildDefectStats(Integer projectId, String startDate, String endDate) {
        DefectStats stats = new DefectStats();

        List<Map<String, Object>> statusCount = defectMapper.countByStatus(projectId);
        Map<String, Integer> statusMap = statusCount.stream()
                .collect(Collectors.toMap(m -> (String) m.get("status"), m -> ((Long) m.get("count")).intValue()));
        stats.setByStatus(statusMap);

        List<Map<String, Object>> severityCount = defectMapper.countBySeverity(projectId);
        Map<String, Integer> severityMap = severityCount.stream()
                .collect(Collectors.toMap(m -> (String) m.get("severity"), m -> ((Long) m.get("count")).intValue()));
        stats.setBySeverity(severityMap);

        List<Map<String, Object>> priorityCount = defectMapper.countByPriority(projectId);
        Map<String, Integer> priorityMap = priorityCount.stream()
                .collect(Collectors.toMap(m -> (String) m.get("priority"), m -> ((Long) m.get("count")).intValue()));
        stats.setByPriority(priorityMap);

        List<Map<String, Object>> defectTrend = defectMapper.countByCreateDate(projectId);
        List<DefectTrendItem> trend = defectTrend.stream()
                .map(m -> {
                    DefectTrendItem item = new DefectTrendItem();
                    item.setDate(m.get("date").toString());
                    item.setCount(((Long) m.get("count")).intValue());
                    return item;
                })
                .sorted(Comparator.comparing(DefectTrendItem::getDate))
                .collect(Collectors.toList());
        stats.setTrend(trend);

        return stats;
    }

    private Date parseDate(String dateStr) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Shanghai"));
            return sdf.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    private ProjectReportStats.ApiTestStats getApiTestStats(Integer projectId, String startDate, String endDate) {
        ProjectReportStats.ApiTestStats stats = new ProjectReportStats.ApiTestStats();
        // 1. 总用例数
        Integer total = apiTestCaseMapper.countByProjectId(projectId);
        stats.setTotalCases(total == null ? 0 : total);
        if (total == null || total == 0) {
            stats.setExecutedCases(0);
            stats.setPassedCases(0);
            stats.setPassRate(0.0);
            return stats;
        }
        // 2. 获取该项目下所有接口测试用例ID
        List<Integer> caseIds = apiTestCaseMapper.findIdsByProjectId(projectId);
        if (caseIds == null || caseIds.isEmpty()) {
            stats.setExecutedCases(0);
            stats.setPassedCases(0);
            stats.setPassRate(0.0);
            return stats;
        }
        // 3. 获取这些用例的最新执行记录（不考虑时间范围，因为执行记录本身有时间）
        List<ApiTestExecution> latestExecutions = apiTestExecutionMapper.findLatestByCaseIds(caseIds);
        // 已执行用例数 = 有执行记录的用例数
        int executed = latestExecutions.size();
        stats.setExecutedCases(executed);
        // 通过用例数 = 最新执行记录中 assertResult = true 的数量
        long passed = latestExecutions.stream().filter(ApiTestExecution::getAssertResult).count();
        stats.setPassedCases((int) passed);
        // 通过率 = 通过用例数 / 总用例数
        double passRate = (total == 0) ? 0.0 : (passed * 100.0 / total);
        stats.setPassRate(passRate);
        return stats;
    }
}