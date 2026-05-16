package com.example.smarttestplatform.module.testexecution.service.impl;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.ProjectUtils;
import com.example.smarttestplatform.module.testexecution.entity.TestExecution;
import com.example.smarttestplatform.module.testexecution.entity.TestExecutionAttachment;
import com.example.smarttestplatform.module.testexecution.mapper.TestExecutionAttachmentMapper;
import com.example.smarttestplatform.module.testexecution.mapper.TestExecutionMapper;
import com.example.smarttestplatform.module.testexecution.service.TestExecutionService;
import com.example.smarttestplatform.module.testplan.entity.TestPlan;
import com.example.smarttestplatform.module.testplan.mapper.TestPlanMapper;
import com.example.smarttestplatform.module.testcase.mapper.TestCaseMapper;
import com.example.smarttestplatform.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TestExecutionServiceImpl implements TestExecutionService {

    @Autowired
    private TestExecutionMapper testExecutionMapper;
    @Autowired
    private TestPlanMapper testPlanMapper;
    @Autowired
    private TestCaseMapper testCaseMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProjectUtils projectUtils;
    @Autowired
    private TestExecutionAttachmentMapper attachmentMapper;
    @Override
    @Transactional
    public void createExecution(TestExecution execution, Integer executorId) {
        // 通过计划ID获取项目ID
        TestPlan plan = testPlanMapper.findById(execution.getPlanId());
        if (plan != null) {
            projectUtils.checkNotArchived(plan.getProjectId());
        }
        execution.setExecutorId(executorId);
        execution.setExecuteTime(new Date());
        testExecutionMapper.insert(execution);
        // 保存附件列表（如果有）
        if (execution.getAttachments() != null) {
            for (TestExecutionAttachment att : execution.getAttachments()) {
                att.setExecutionId(execution.getId());
                attachmentMapper.insert(att);
            }
        }
    }

    @Override
    public PageResult<TestExecution> pageQuery(PageRequest pageRequest) {
        Map<String, Object> conditions = pageRequest.getConditions();
        int offset = (pageRequest.getPage() - 1) * pageRequest.getSize();
        List<TestExecution> records = testExecutionMapper.pageQuery(conditions, offset, pageRequest.getSize());
        for (TestExecution exec : records) {
            if (exec.getPlanId() != null) {
                exec.setPlanName(testPlanMapper.findById(exec.getPlanId()).getPlanName());
            }
            if (exec.getCaseId() != null) {
                exec.setCaseTitle(testCaseMapper.findById(exec.getCaseId()).getTitle());
            }
            if (exec.getExecutorId() != null) {
                exec.setExecutorName(userMapper.findById(exec.getExecutorId()).getRealName());
            }
            // 在 pageQuery 的 for 循环中添加
            exec.setAttachments(attachmentMapper.findByExecutionId(exec.getId()));
        }
        Long total = testExecutionMapper.count(conditions);
        return new PageResult<>(records, total, pageRequest.getPage(), pageRequest.getSize());
    }

    @Override
    public TestExecution getLatestExecution(Integer planId, Integer caseId) {
        return testExecutionMapper.findLatestByPlanAndCase(planId, caseId);
    }

    @Override
    public Map<String, Integer> getStatisticsByPlan(Integer planId) {
        List<Map<String, Object>> list = testExecutionMapper.countByPlanId(planId);
        return list.stream().collect(Collectors.toMap(
                m -> (String) m.get("result"),
                m -> ((Long) m.get("count")).intValue()
        ));
    }
}