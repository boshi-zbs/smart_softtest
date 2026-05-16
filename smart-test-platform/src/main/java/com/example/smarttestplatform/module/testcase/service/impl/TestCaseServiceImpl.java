package com.example.smarttestplatform.module.testcase.service.impl;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.ProjectUtils;
import com.example.smarttestplatform.module.projectmodule.mapper.ProjectModuleMapper;
import com.example.smarttestplatform.module.testplan.mapper.PlanCaseMapper; // 假设有
import com.example.smarttestplatform.module.project.mapper.ProjectMapper;
import com.example.smarttestplatform.module.projectmodule.entity.ProjectModule;
import com.example.smarttestplatform.module.requirement.entity.Requirement;
import com.example.smarttestplatform.module.requirement.mapper.RequirementMapper;
import com.example.smarttestplatform.module.testcase.entity.TestCase;
import com.example.smarttestplatform.module.testcase.mapper.TestCaseMapper;
import com.example.smarttestplatform.module.testcase.service.TestCaseService;
import com.example.smarttestplatform.module.testexecution.entity.TestExecution;
import com.example.smarttestplatform.module.testexecution.mapper.TestExecutionMapper;
import com.example.smarttestplatform.module.testplan.entity.TestPlan;
import com.example.smarttestplatform.module.testplan.mapper.TestPlanMapper;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TestCaseServiceImpl implements TestCaseService {

    @Autowired
    private TestCaseMapper testCaseMapper;
    @Autowired
    private RequirementMapper requirementMapper;
    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private TestPlanMapper testPlanMapper;
    @Autowired
    private PlanCaseMapper planCaseMapper;
    @Autowired
    private TestExecutionMapper testExecutionMapper;
    @Autowired
    private ProjectModuleMapper projectModuleMapper;
    @Autowired
    private ProjectUtils projectUtils;
    @Override
    public TestCase findById(Integer id) {
        TestCase tc = testCaseMapper.findById(id);
        enrichTestCase(tc);
        return tc;
    }

    @Override
    public PageResult<TestCase> pageQuery(PageRequest pageRequest) {
        Map<String, Object> conditions = pageRequest.getConditions();
        int offset = (pageRequest.getPage() - 1) * pageRequest.getSize();
        List<TestCase> records = testCaseMapper.pageQuery(conditions, offset, pageRequest.getSize());
        for (TestCase tc : records) {
            enrichTestCase(tc);
        }
        Long total = testCaseMapper.count(conditions);
        return new PageResult<>(records, total, pageRequest.getPage(), pageRequest.getSize());
    }

    private void enrichTestCase(TestCase tc) {
        if (tc == null) return;
        // 填充需求标题
        if (tc.getRequirementId() != null) {
            Requirement req = requirementMapper.findById(tc.getRequirementId());
            if (req != null) tc.setRequirementTitle(req.getTitle());
        }
        if (tc.getModuleId() != null) {
            ProjectModule module = projectModuleMapper.findById(tc.getModuleId());
            if (module != null) {
                tc.setModuleName(module.getModuleName());
            }
        }
        // 填充创建人姓名
        if (tc.getCreatorId() != null) {
            User user = userMapper.findById(tc.getCreatorId());
            if (user != null) tc.setCreatorName(user.getRealName() != null ? user.getRealName() : user.getUsername());
        }
        // 填充最近一次执行结果
        TestExecution latest = testExecutionMapper.findLatestByCaseId(tc.getId()); // 需要在 TestExecutionMapper 中添加
        if (latest != null) {
            tc.setLastResult(latest.getResult());
            tc.setActualResult(latest.getActualResult());
        }
        // 填充关联的计划名称列表
        List<Integer> planIds = planCaseMapper.findPlanIdsByCaseId(tc.getId());
        if (!planIds.isEmpty()) {
            List<String> planNames = planIds.stream()
                    .map(id -> {
                        TestPlan plan = testPlanMapper.findById(id);
                        return plan != null ? plan.getPlanName() : null;
                    })
                    .filter(name -> name != null)
                    .collect(Collectors.toList());
            tc.setPlanNames(planNames);
        }
    }

    @Override
    @Transactional
    public void createTestCase(TestCase testCase, Integer creatorId) {
        // 如果直接传入了 projectId，校验之
        if (testCase.getProjectId() != null) {
            projectUtils.checkNotArchived(testCase.getProjectId());
        }
        // 如果传入了 planId，则通过计划获取项目ID
        if (testCase.getPlanId() != null) {
            TestPlan plan = testPlanMapper.findById(testCase.getPlanId());
            if (plan != null) {
                projectUtils.checkNotArchived(plan.getProjectId());
            }
        }

        if (testCase.getPlanId() != null) {
            TestPlan plan = testPlanMapper.findById(testCase.getPlanId());
            if (plan == null) {
                throw new RuntimeException("测试计划不存在");
            }
            // 校验需求
            if (testCase.getRequirementId() != null) {
                Requirement req = requirementMapper.findById(testCase.getRequirementId());
                if (req == null) {
                    throw new RuntimeException("需求不存在");
                }
                if (!req.getProjectId().equals(plan.getProjectId())) {
                    throw new RuntimeException("需求所属项目与计划所属项目不一致");
                }
            }
            // 校验模块
            if (testCase.getModuleId() != null) {
                ProjectModule module = projectModuleMapper.findById(testCase.getModuleId());
                if (module == null) {
                    throw new RuntimeException("模块不存在");
                }
                if (!module.getProjectId().equals(plan.getProjectId())) {
                    throw new RuntimeException("模块所属项目与计划所属项目不一致");
                }
            }
            // 设置项目ID
            testCase.setProjectId(plan.getProjectId());
        }

        testCase.setCreatorId(creatorId);
        testCaseMapper.insert(testCase);

        // 如果传入了计划ID，建立关联
        if (testCase.getPlanId() != null) {
            planCaseMapper.insertPlanCase(testCase.getPlanId(), testCase.getId());
        }
    }
    @Override
    @Transactional
    public void updateTestCase(TestCase testCase) {
        // 更新时，从原用例中获取项目ID（用例表有 project_id 字段）
        TestCase old = testCaseMapper.findById(testCase.getId());
        if (old != null) {
            projectUtils.checkNotArchived(old.getProjectId());
        }
        // 编辑时不允许修改计划，所以忽略 planId
        // 但需要校验需求是否与已有计划的项目一致（可选，可以通过当前用例关联的计划来检查）
        // 为了简化，这里不校验，假设需求属于同一项目
        testCaseMapper.update(testCase);
    }

    @Override
    @Transactional
    public void deleteTestCase(Integer id) {
        TestCase tc = testCaseMapper.findById(id);
        if (tc != null) {
            projectUtils.checkNotArchived(tc.getProjectId());
        }

        testCaseMapper.deleteById(id);
        // 计划关联会级联删除（因外键 ON DELETE CASCADE，或手动）
    }

    @Override
    @Transactional
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Integer id : ids) {
            testCaseMapper.deleteById(id);
        }
    }

    @Override
    @Transactional
    public void batchCreate(List<TestCase> cases, Integer planId, Integer creatorId) {
        if (cases == null || cases.isEmpty()) return;
        TestPlan plan = testPlanMapper.findById(planId);
        if (plan == null) throw new RuntimeException("测试计划不存在");
        projectUtils.checkNotArchived(plan.getProjectId());

        for (TestCase tc : cases) {
            tc.setProjectId(plan.getProjectId());
            tc.setCreatorId(creatorId);
            tc.setStatus("有效");   // ✅ 强制设置状态为“有效”
            testCaseMapper.insert(tc);
            planCaseMapper.insertPlanCase(planId, tc.getId());
        }
    }
}