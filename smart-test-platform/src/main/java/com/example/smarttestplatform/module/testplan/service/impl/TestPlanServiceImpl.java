package com.example.smarttestplatform.module.testplan.service.impl;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.ProjectUtils;
import com.example.smarttestplatform.module.message.entity.Message;
import com.example.smarttestplatform.module.message.service.MessageService;
import com.example.smarttestplatform.module.project.entity.Project;
import com.example.smarttestplatform.module.project.entity.ProjectMember;
import com.example.smarttestplatform.module.project.mapper.ProjectMapper;
import com.example.smarttestplatform.module.project.mapper.ProjectMemberMapper;
import com.example.smarttestplatform.module.testexecution.entity.TestExecution;
import com.example.smarttestplatform.module.testexecution.mapper.TestExecutionMapper;
import com.example.smarttestplatform.module.testplan.entity.PlanCase;
import com.example.smarttestplatform.module.testplan.entity.TestPlan;
import com.example.smarttestplatform.module.testplan.mapper.PlanCaseMapper;
import com.example.smarttestplatform.module.testplan.mapper.TestPlanMapper;
import com.example.smarttestplatform.module.testplan.service.TestPlanService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class TestPlanServiceImpl implements TestPlanService {

    @Autowired
    private TestPlanMapper testPlanMapper;
    @Autowired
    private PlanCaseMapper planCaseMapper;
    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProjectUtils projectUtils;

    // 在 TestPlanServiceImpl 中注入 TestExecutionMapper
    @Autowired
    private TestExecutionMapper testExecutionMapper;
    @Autowired
    private ProjectMemberMapper projectMemberMapper;
    @Autowired
    private MessageService messageService;

    @Override
    public TestPlan findById(Integer id) {
        return testPlanMapper.findById(id);
    }

    @Override
    public PageResult<TestPlan> pageQuery(PageRequest pageRequest) {
        Map<String, Object> conditions = pageRequest.getConditions();
        int offset = (pageRequest.getPage() - 1) * pageRequest.getSize();
        List<TestPlan> records = testPlanMapper.pageQuery(conditions, offset, pageRequest.getSize());
        // 填充项目名称和创建人姓名
        for (TestPlan plan : records) {
            if (plan.getProjectId() != null) {
                Project project = projectMapper.findById(plan.getProjectId());
                if (project != null) {
                    plan.setProjectName(project.getProjectName());
                }
            }
            if (plan.getCreatorId() != null) {
                User user = userMapper.findById(plan.getCreatorId());
                if (user != null) {
                    plan.setCreatorName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                }
            }
        }
        Long total = testPlanMapper.count(conditions);
        return new PageResult<>(records, total, pageRequest.getPage(), pageRequest.getSize());
    }

    @Override
    @Transactional
    public void createTestPlan(TestPlan testPlan, Integer creatorId) {
        projectUtils.checkNotArchived(testPlan.getProjectId());
        testPlan.setCreatorId(creatorId);
        testPlanMapper.insert(testPlan);
    }

    @Override
    @Transactional
    public void updateTestPlan(TestPlan testPlan) {
        TestPlan old = testPlanMapper.findById(testPlan.getId());
        if (old != null) {
            projectUtils.checkNotArchived(old.getProjectId());
        }
        testPlanMapper.update(testPlan);
    }

    @Override
    @Transactional
    public void deleteTestPlan(Integer id) {
        TestPlan plan = testPlanMapper.findById(id);
        if (plan != null) {
            projectUtils.checkNotArchived(plan.getProjectId());
        }
        // 级联删除关联（数据库外键已设 CASCADE，但也可以手动删）
        testPlanMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Integer id : ids) {
            testPlanMapper.deleteById(id);
        }
    }

    @Override
    @Transactional
    public void addCaseToPlan(Integer planId, Integer caseId) {
        // 检查是否已存在
        PlanCase existing = planCaseMapper.findByPlanAndCase(planId, caseId);
        if (existing != null) {
            throw new RuntimeException("该用例已关联到此计划，不能重复添加");
        }

        TestPlan plan = testPlanMapper.findById(planId);
        if (plan == null) throw new RuntimeException("计划不存在");

        projectUtils.checkNotArchived(plan.getProjectId());

        planCaseMapper.insertPlanCase(planId, caseId);

        // 向项目下所有测试人员发送待办消息
        List<ProjectMember> testers = projectMemberMapper.findByProjectIdAndRole(plan.getProjectId(), "测试人员");
        Integer operatorId = getCurrentUserId();
        for (ProjectMember tester : testers) {
            if (tester.getUserId().equals(operatorId)) continue;
            Message msg = new Message();
            msg.setSenderId(operatorId);
            msg.setReceiverId(tester.getUserId());
            msg.setTitle("新的测试任务");
            msg.setContent("计划【" + plan.getPlanName() + "】新增了测试用例，请及时执行");
            msg.setType("待办");
            msg.setRelatedId(planId);
            msg.setRelatedType("testplan");
            msg.setIsRead(false);
            msg.setStatus("pending");
            msg.setCreateTime(new Date());
            messageService.sendMessage(msg);
        }
    }

    @Override
    @Transactional
    public void removeCaseFromPlan(Integer planId, Integer caseId) {
        TestPlan plan = testPlanMapper.findById(planId);
        if (plan != null) {
            projectUtils.checkNotArchived(plan.getProjectId());
        }
        planCaseMapper.deletePlanCase(planId, caseId);
    }

    @Override
    public List<Integer> getCaseIdsByPlanId(Integer planId) {
        return planCaseMapper.findCaseIdsByPlanId(planId);
    }

    @Override
    public TestPlan getPlanWithProgress(Integer planId) {
        TestPlan plan = testPlanMapper.findById(planId);
        if (plan == null) return null;
        enrichPlanWithProgress(plan);
        return plan;
    }

    @Override
    public PageResult<TestPlan> pageQueryWithProgress(PageRequest pageRequest) {
        PageResult<TestPlan> pageResult = pageQuery(pageRequest);
        for (TestPlan plan : pageResult.getRecords()) {
            enrichPlanWithProgress(plan);
        }
        return pageResult;
    }

    private void enrichPlanWithProgress(TestPlan plan) {
        // 获取计划关联的用例总数
        List<Integer> caseIds = planCaseMapper.findCaseIdsByPlanId(plan.getId());
        int total = caseIds.size();
        plan.setTotalCases(total);

        // 获取该计划下所有用例的最新执行结果
        int executed = 0, passed = 0, failed = 0;
        for (Integer caseId : caseIds) {
            TestExecution latest = testExecutionMapper.findLatestByPlanAndCase(plan.getId(), caseId);
            if (latest != null) {
                executed++;
                if ("通过".equals(latest.getResult())) passed++;
                else if ("失败".equals(latest.getResult())) failed++;
            }
        }
        plan.setExecutedCases(executed);
        plan.setPassedCases(passed);
        plan.setFailedCases(failed);
        plan.setProgress(total == 0 ? 0 : (double) executed / total * 100);
    }

    private Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            User user = userMapper.findByUsername(userDetails.getUsername());
            return user != null ? user.getId() : null;
        }
        return null;
    }
}