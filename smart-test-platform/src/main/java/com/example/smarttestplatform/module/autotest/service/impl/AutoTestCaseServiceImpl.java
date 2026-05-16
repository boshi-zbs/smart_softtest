package com.example.smarttestplatform.module.autotest.service.impl;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.ProjectUtils;
import com.example.smarttestplatform.module.autotest.dto.ExecuteResult;
import com.example.smarttestplatform.module.autotest.entity.AutoTestCase;
import com.example.smarttestplatform.module.autotest.entity.AutoTestExecution;
import com.example.smarttestplatform.module.autotest.entity.AutoTestStep;
import com.example.smarttestplatform.module.autotest.mapper.AutoTestCaseMapper;
import com.example.smarttestplatform.module.autotest.mapper.AutoTestExecutionMapper;
import com.example.smarttestplatform.module.autotest.mapper.AutoTestStepMapper;
import com.example.smarttestplatform.module.autotest.service.AutoTestCaseService;
import com.example.smarttestplatform.module.autotest.util.SeleniumExecutor;
import com.example.smarttestplatform.module.message.entity.Message;
import com.example.smarttestplatform.module.message.service.MessageService;
import com.example.smarttestplatform.module.project.entity.Project;
import com.example.smarttestplatform.module.project.entity.ProjectMember;
import com.example.smarttestplatform.module.project.mapper.ProjectMapper;
import com.example.smarttestplatform.module.project.mapper.ProjectMemberMapper;
import com.example.smarttestplatform.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class AutoTestCaseServiceImpl implements AutoTestCaseService {

    @Autowired
    private AutoTestCaseMapper testCaseMapper;
    @Autowired
    private AutoTestStepMapper stepMapper;
    @Autowired
    private AutoTestExecutionMapper executionMapper;
    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SeleniumExecutor seleniumExecutor;
    @Autowired
    private ProjectUtils projectUtils;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ProjectMemberMapper projectMemberMapper;

    private void enrichTestCase(AutoTestCase testCase) {
        if (testCase == null) return;
        if (testCase.getProjectId() != null) {
            Project project = projectMapper.findById(testCase.getProjectId());
            if (project != null) testCase.setProjectName(project.getProjectName());
        }
        List<AutoTestStep> steps = stepMapper.findByCaseId(testCase.getId());
        testCase.setSteps(steps);
    }

    @Override
    public AutoTestCase findById(Integer id) {
        AutoTestCase testCase = testCaseMapper.findById(id);
        enrichTestCase(testCase);
        return testCase;
    }

    @Override
    public PageResult<AutoTestCase> pageQuery(PageRequest pageRequest) {
        Map<String, Object> conditions = pageRequest.getConditions();
        int offset = (pageRequest.getPage() - 1) * pageRequest.getSize();
        List<AutoTestCase> records = testCaseMapper.pageQuery(conditions, offset, pageRequest.getSize());
        for (AutoTestCase tc : records) {
            enrichTestCase(tc);
        }
        Long total = testCaseMapper.count(conditions);
        return new PageResult<>(records, total, pageRequest.getPage(), pageRequest.getSize());
    }

    @Override
    @Transactional
    public void createCase(AutoTestCase testCase, List<AutoTestStep> steps, Integer createUserId) {
        projectUtils.checkNotArchived(testCase.getProjectId());
        testCase.setCreateUserId(createUserId);
        testCase.setStatus(1);
        if (testCase.getHeadless() == null) {
            testCase.setHeadless(1);
        }
        testCaseMapper.insert(testCase);
        if (steps != null) {
            for (AutoTestStep step : steps) {
                step.setCaseId(testCase.getId());
                stepMapper.insert(step);
            }
        }
    }

    @Override
    @Transactional
    public void updateCase(AutoTestCase testCase, List<AutoTestStep> steps) {
        AutoTestCase old = testCaseMapper.findById(testCase.getId());
        if (old != null) {
            projectUtils.checkNotArchived(old.getProjectId());
        }
        testCaseMapper.update(testCase);
        stepMapper.deleteByCaseId(testCase.getId());
        if (steps != null) {
            for (AutoTestStep step : steps) {
                step.setCaseId(testCase.getId());
                stepMapper.insert(step);
            }
        }
    }

    @Override
    @Transactional
    public void deleteCase(Integer id) {
        AutoTestCase tc = testCaseMapper.findById(id);
        if (tc != null) {
            projectUtils.checkNotArchived(tc.getProjectId());
        }
        testCaseMapper.deleteById(id);
    }

    @Override
    public Integer executeCaseAndReturnId(Integer caseId, Integer executorId, String ip, Boolean headless) {
        AutoTestCase testCase = findById(caseId);
        if (testCase == null) throw new RuntimeException("用例不存在");

        projectUtils.checkNotArchived(testCase.getProjectId());

        AutoTestExecution execution = new AutoTestExecution();
        execution.setCaseId(caseId);
        execution.setExecutorId(executorId);
        execution.setStartTime(new Date());
        execution.setStatus("running");
        execution.setIp(ip);
        executionMapper.insert(execution);

        final Integer executionId = execution.getId();
        final boolean isHeadless = headless != null ? headless : true;
        final Integer finalCaseId = caseId;

        new Thread(() -> {
            ExecuteResult result;
            try {
                result = seleniumExecutor.execute(testCase, testCase.getSteps(), isHeadless);
                execution.setStatus(result.isSuccess() ? "success" : "failed");
                execution.setResult(result.getMessage());
                // 保存截图 URL
                execution.setScreenshotUrl(result.getScreenshotUrl());
            } catch (Exception e) {
                execution.setStatus("failed");
                execution.setResult("执行异常：" + e.getMessage());
                result = new ExecuteResult(false, execution.getResult(), null);
            } finally {
                execution.setEndTime(new Date());
                executionMapper.update(execution);
            }

            // 如果执行失败，通知项目下所有开发人员
            if (!result.isSuccess()) {
                List<ProjectMember> devs = projectMemberMapper.findByProjectIdAndRole(testCase.getProjectId(), "开发人员");
                for (ProjectMember dev : devs) {
                    if (dev.getUserId().equals(executorId)) continue;
                    Message msg = new Message();
                    msg.setSenderId(executorId);
                    msg.setReceiverId(dev.getUserId());
                    msg.setTitle("自动化测试失败");
                    msg.setContent("用例【" + testCase.getCaseName() + "】执行失败，请修复");
                    msg.setType("待办");
                    msg.setRelatedId(finalCaseId);
                    msg.setRelatedType("autotest");
                    msg.setIsRead(false);
                    msg.setStatus("pending");
                    msg.setCreateTime(new Date());
                    messageService.sendMessage(msg);
                }
            }
        }).start();

        return executionId;
    }

    @Override
    public ExecuteResult executeCase(Integer caseId, Integer executorId, String ip, Boolean headless) {
        AutoTestCase testCase = findById(caseId);
        if (testCase == null) throw new RuntimeException("用例不存在");

        AutoTestExecution execution = new AutoTestExecution();
        execution.setCaseId(caseId);
        execution.setExecutorId(executorId);
        execution.setStartTime(new Date());
        execution.setStatus("running");
        execution.setIp(ip);
        executionMapper.insert(execution);

        ExecuteResult result;
        try {
            boolean isHeadless = headless != null ? headless : true;
            result = seleniumExecutor.execute(testCase, testCase.getSteps(), isHeadless);
            execution.setStatus(result.isSuccess() ? "success" : "failed");
            execution.setResult(result.getMessage());
            execution.setScreenshotUrl(result.getScreenshotUrl());
        } catch (Exception e) {
            execution.setStatus("failed");
            execution.setResult("执行异常：" + e.getMessage());
            result = new ExecuteResult(false, execution.getResult(), null);
        } finally {
            execution.setEndTime(new Date());
            executionMapper.update(execution);
        }
        return result;
    }
}