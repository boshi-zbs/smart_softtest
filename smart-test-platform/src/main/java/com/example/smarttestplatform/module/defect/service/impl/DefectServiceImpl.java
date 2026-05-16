package com.example.smarttestplatform.module.defect.service.impl;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.ProjectUtils;
import com.example.smarttestplatform.module.defect.entity.Defect;
import com.example.smarttestplatform.module.defect.entity.DefectAttachment;
import com.example.smarttestplatform.module.defect.entity.DefectComment;
import com.example.smarttestplatform.module.defect.mapper.DefectAttachmentMapper;
import com.example.smarttestplatform.module.defect.mapper.DefectCommentMapper;
import com.example.smarttestplatform.module.defect.mapper.DefectMapper;
import com.example.smarttestplatform.module.defect.service.DefectService;
import com.example.smarttestplatform.module.message.entity.Message;
import com.example.smarttestplatform.module.message.service.MessageService;
import com.example.smarttestplatform.module.project.entity.Project;
import com.example.smarttestplatform.module.project.mapper.ProjectMapper;
import com.example.smarttestplatform.module.requirement.entity.Requirement;
import com.example.smarttestplatform.module.requirement.mapper.RequirementMapper;
import com.example.smarttestplatform.module.testcase.entity.TestCase;
import com.example.smarttestplatform.module.testcase.mapper.TestCaseMapper;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DefectServiceImpl implements DefectService {

    @Autowired
    private DefectMapper defectMapper;
    @Autowired
    private DefectCommentMapper defectCommentMapper;
    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private TestCaseMapper testCaseMapper;
    @Autowired
    private RequirementMapper requirementMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ProjectUtils projectUtils;
    @Autowired
    private DefectAttachmentMapper defectAttachmentMapper;

    private void enrichDefect(Defect defect) {
        if (defect == null) return;
        if (defect.getProjectId() != null) {
            Project p = projectMapper.findById(defect.getProjectId());
            if (p != null) defect.setProjectName(p.getProjectName());
        }
        if (defect.getReporterId() != null) {
            User u = userMapper.findById(defect.getReporterId());
            if (u != null) defect.setReporterName(u.getRealName() != null ? u.getRealName() : u.getUsername());
        }
        if (defect.getAssigneeId() != null) {
            User u = userMapper.findById(defect.getAssigneeId());
            if (u != null) defect.setAssigneeName(u.getRealName() != null ? u.getRealName() : u.getUsername());
        }
        if (defect.getTestCaseId() != null) {
            TestCase tc = testCaseMapper.findById(defect.getTestCaseId());
            if (tc != null) defect.setTestCaseTitle(tc.getTitle());
        }
        if (defect.getRequirementId() != null) {
            Requirement r = requirementMapper.findById(defect.getRequirementId());
            if (r != null) defect.setRequirementTitle(r.getTitle());
        }
        // ✅ 填充附件列表
        if (defect.getId() != null) {
            defect.setAttachments(defectAttachmentMapper.findByDefectId(defect.getId()));
        }
    }

    @Override
    public Defect findById(Integer id) {
        Defect defect = defectMapper.findById(id);
        enrichDefect(defect);
        return defect;
    }

    @Override
    public PageResult<Defect> pageQuery(PageRequest pageRequest) {
        Map<String, Object> conditions = pageRequest.getConditions();
        int offset = (pageRequest.getPage() - 1) * pageRequest.getSize();
        List<Defect> records = defectMapper.pageQuery(conditions, offset, pageRequest.getSize());
        for (Defect d : records) enrichDefect(d);
        Long total = defectMapper.count(conditions);
        return new PageResult<>(records, total, pageRequest.getPage(), pageRequest.getSize());
    }

    @Override
    @Transactional
    public void createDefect(Defect defect, Integer reporterId) {
        projectUtils.checkNotArchived(defect.getProjectId());
        // 如果传入了 testCaseId，查询用例并自动填充需求ID和项目ID（如果前端未提供项目ID）
        if (defect.getTestCaseId() != null) {
            TestCase testCase = testCaseMapper.findById(defect.getTestCaseId());
            if (testCase != null) {
                // 如果前端没有传递 projectId，则从用例获取
                if (defect.getProjectId() == null) {
                    defect.setProjectId(testCase.getProjectId());
                }
                // 自动关联需求（如果用例有关联需求）
                if (testCase.getRequirementId() != null) {
                    defect.setRequirementId(testCase.getRequirementId());
                }
            }
        }

        defect.setReporterId(reporterId);
        defect.setStatus("新建");
        defect.setCreateTime(new Date());
        defectMapper.insert(defect);
        // ✅ 保存附件列表
        if (defect.getAttachments() != null) {
            for (DefectAttachment att : defect.getAttachments()) {
                att.setDefectId(defect.getId());
                defectAttachmentMapper.insert(att);
            }
        }
        // 如果有指派人，发送消息（可选）
        if (defect.getAssigneeId() != null) {
            sendMessage(reporterId, defect.getAssigneeId(),
                    "新的缺陷指派给您",
                    "缺陷 #" + defect.getId() + "：" + defect.getTitle(),
                    "通知", defect.getId(), "defect");
        }
    }

    @Override
    @Transactional
    public void updateDefect(Defect defect) {
        // 获取旧缺陷信息
        Defect oldDefect = defectMapper.findById(defect.getId());
        if (oldDefect == null) {
            throw new RuntimeException("缺陷不存在");
        }

        // 校验项目是否归档
        projectUtils.checkNotArchived(oldDefect.getProjectId());

        // 更新缺陷
        defectMapper.update(defect);

        // ✅ 更新附件：先删除旧附件，再插入新附件（简单处理）
        defectAttachmentMapper.deleteByDefectId(defect.getId());
        if (defect.getAttachments() != null) {
            for (DefectAttachment att : defect.getAttachments()) {
                att.setDefectId(defect.getId());
                defectAttachmentMapper.insert(att);
            }
        }
        // 指派人发生变化时发送待办消息
        if (oldDefect.getAssigneeId() != null && defect.getAssigneeId() != null
                && !oldDefect.getAssigneeId().equals(defect.getAssigneeId())) {
            Integer currentUserId = getCurrentUserId();
            // 避免给自己发消息
            if (!currentUserId.equals(defect.getAssigneeId())) {
                Message msg = new Message();
                msg.setSenderId(currentUserId);
                msg.setReceiverId(defect.getAssigneeId());
                msg.setTitle("待处理缺陷");
                msg.setContent("您被指派处理缺陷：" + defect.getTitle());
                msg.setType("待办");
                msg.setRelatedId(defect.getId());
                msg.setRelatedType("defect");
                msg.setIsRead(false);
                msg.setStatus("pending");   // 待办状态初始为 pending
                msg.setCreateTime(new Date());
                messageService.sendMessage(msg);
            }
        }
    }
    @Override
    @Transactional
    public void deleteDefect(Integer id) {
        Defect defect = defectMapper.findById(id);
        if (defect != null) {
            projectUtils.checkNotArchived(defect.getProjectId());
        }
        defectMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Integer id : ids) defectMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void addComment(Integer defectId, Integer userId, String content) {
        DefectComment comment = new DefectComment();
        comment.setDefectId(defectId);
        comment.setUserId(userId);
        comment.setAction("评论");
        comment.setContent(content);
        defectCommentMapper.insert(comment);
    }

    @Override
    public List<DefectComment> getComments(Integer defectId) {
        List<DefectComment> list = defectCommentMapper.findByDefectId(defectId);
        for (DefectComment c : list) {
            if (c.getUserId() != null) {
                User u = userMapper.findById(c.getUserId());
                if (u != null) c.setUserName(u.getRealName() != null ? u.getRealName() : u.getUsername());
            }
        }
        return list;
    }

    @Override
    @Transactional
    public void changeStatus(Integer defectId, Integer userId, String newStatus, String comment) {
        Defect defect = defectMapper.findById(defectId);
        if (defect == null) throw new RuntimeException("缺陷不存在");

        projectUtils.checkNotArchived(defect.getProjectId());

        String oldStatus = defect.getStatus();
        defect.setStatus(newStatus);
        defectMapper.update(defect);

        // 记录评论
        DefectComment dc = new DefectComment();
        dc.setDefectId(defectId);
        dc.setUserId(userId);
        dc.setAction("状态变更");
        dc.setOldValue(oldStatus);
        dc.setNewValue(newStatus);
        dc.setContent(comment);
        defectCommentMapper.insert(dc);

        // 发送通知（非待办）
        Integer receiverId = null;
        if ("已修复".equals(newStatus)) {
            receiverId = defect.getReporterId();
        } else if ("驳回".equals(newStatus)) {
            receiverId = defect.getReporterId();
        } else if ("已关闭".equals(newStatus)) {
            receiverId = defect.getAssigneeId();
        }
        if (receiverId != null && !receiverId.equals(userId)) {
            Message msg = new Message();
            msg.setSenderId(userId);
            msg.setReceiverId(receiverId);
            msg.setTitle("缺陷状态变更");
            msg.setContent("缺陷 #" + defectId + " 状态由 " + oldStatus + " 变为 " + newStatus);
            msg.setType("通知");
            msg.setRelatedId(defectId);
            msg.setRelatedType("defect");
            msg.setIsRead(false);
            msg.setStatus(null);   // 通知类型不需要 pending/completed
            msg.setCreateTime(new Date());
            messageService.sendMessage(msg);
        }
    }

    @Override
    public Map<String, Integer> countByStatus(Integer projectId) {
        List<Map<String, Object>> list = defectMapper.countByStatus(projectId);
        return list.stream().collect(Collectors.toMap(
                m -> (String) m.get("status"),
                m -> ((Long) m.get("count")).intValue()
        ));
    }

    @Override
    public Map<String, Integer> countBySeverity(Integer projectId) {
        List<Map<String, Object>> list = defectMapper.countBySeverity(projectId);
        return list.stream().collect(Collectors.toMap(
                m -> (String) m.get("severity"),
                m -> ((Long) m.get("count")).intValue()
        ));
    }

    @Override
    public Map<String, Integer> countByPriority(Integer projectId) {
        List<Map<String, Object>> list = defectMapper.countByPriority(projectId);
        return list.stream().collect(Collectors.toMap(
                m -> (String) m.get("priority"),
                m -> ((Long) m.get("count")).intValue()
        ));
    }

    private void sendMessage(Integer senderId, Integer receiverId, String title, String content,
                             String type, Integer relatedId, String relatedType) {
        if (receiverId == null || receiverId.equals(senderId)) return;
        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(receiverId);
        msg.setTitle(title);
        msg.setContent(content);
        msg.setType(type);
        msg.setRelatedId(relatedId);
        msg.setRelatedType(relatedType);
        messageService.sendMessage(msg);
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