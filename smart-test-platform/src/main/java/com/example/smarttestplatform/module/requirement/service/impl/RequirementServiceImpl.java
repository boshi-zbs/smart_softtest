package com.example.smarttestplatform.module.requirement.service.impl;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.ProjectUtils;
import com.example.smarttestplatform.module.message.entity.Message;
import com.example.smarttestplatform.module.message.service.MessageService;
import com.example.smarttestplatform.module.project.entity.Project;
import com.example.smarttestplatform.module.project.mapper.ProjectMapper;
import com.example.smarttestplatform.module.requirement.entity.Requirement;
import com.example.smarttestplatform.module.requirement.mapper.RequirementMapper;
import com.example.smarttestplatform.module.requirement.service.RequirementService;
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
import java.util.Objects;

@Service
public class RequirementServiceImpl implements RequirementService {

    @Autowired
    private RequirementMapper requirementMapper;
    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ProjectUtils projectUtils;   // 注入

    @Override
    public Requirement findById(Integer id) {
        Requirement req = requirementMapper.findById(id);
        enrichRequirement(req);
        return req;
    }

    @Override
    public PageResult<Requirement> pageQuery(PageRequest pageRequest) {
        Map<String, Object> conditions = pageRequest.getConditions();
        int offset = (pageRequest.getPage() - 1) * pageRequest.getSize();
        List<Requirement> records = requirementMapper.pageQuery(conditions, offset, pageRequest.getSize());
        for (Requirement req : records) {
            enrichRequirement(req);
        }
        Long total = requirementMapper.count(conditions);
        return new PageResult<>(records, total, pageRequest.getPage(), pageRequest.getSize());
    }

    private void enrichRequirement(Requirement req) {
        if (req == null) return;
        if (req.getProjectId() != null) {
            Project project = projectMapper.findById(req.getProjectId());
            if (project != null) req.setProjectName(project.getProjectName());
        }
        if (req.getAssigneeId() != null) {
            User user = userMapper.findById(req.getAssigneeId());
            if (user != null) req.setAssigneeName(user.getRealName() != null ? user.getRealName() : user.getUsername());
        }
        if (req.getCreatorId() != null) {
            User creator = userMapper.findById(req.getCreatorId());
            if (creator != null) req.setCreatorName(creator.getRealName() != null ? creator.getRealName() : creator.getUsername());
        }
    }

    @Override
    @Transactional
    public void createRequirement(Requirement requirement, Integer creatorId) {
        // 校验项目是否已归档
        projectUtils.checkNotArchived(requirement.getProjectId());
        requirement.setCreatorId(creatorId);
        requirementMapper.insert(requirement);

        // 如果有负责人，发送通知
        if (requirement.getAssigneeId() != null) {
            sendMessage(creatorId, requirement.getAssigneeId(),
                    "新的需求指派给您",
                    "需求 #" + requirement.getId() + "：" + requirement.getTitle(),
                    "通知", requirement.getId(), "requirement");
        }
    }

    @Override
    @Transactional
    public void updateRequirement(Requirement requirement) {
        Requirement old = requirementMapper.findById(requirement.getId());
        if (old == null) throw new RuntimeException("需求不存在");

        projectUtils.checkNotArchived(old.getProjectId());



        requirementMapper.update(requirement);

        // 负责人变更时发送待办消息
        if (old.getAssigneeId() != null && requirement.getAssigneeId() != null
                && !old.getAssigneeId().equals(requirement.getAssigneeId())) {
            Integer currentUserId = getCurrentUserId();
            if (!currentUserId.equals(requirement.getAssigneeId())) {
                Message msg = new Message();
                msg.setSenderId(currentUserId);
                msg.setReceiverId(requirement.getAssigneeId());
                msg.setTitle("待处理需求");
                msg.setContent("您被指派处理需求：" + requirement.getTitle());
                msg.setType("待办");
                msg.setRelatedId(requirement.getId());
                msg.setRelatedType("requirement");
                msg.setIsRead(false);
                msg.setStatus("pending");
                msg.setCreateTime(new Date());
                messageService.sendMessage(msg);
            }
        }

        // 状态变更时发送通知
        if (!old.getStatus().equals(requirement.getStatus()) && requirement.getAssigneeId() != null) {
            String content = "需求状态由 " + old.getStatus() + " 变为 " + requirement.getStatus();
            Message msg = new Message();
            msg.setSenderId(requirement.getCreatorId());
            msg.setReceiverId(requirement.getAssigneeId());
            msg.setTitle("需求状态变更");
            msg.setContent(content);
            msg.setType("通知");
            msg.setRelatedId(requirement.getId());
            msg.setRelatedType("requirement");
            msg.setIsRead(false);
            msg.setStatus(null);
            msg.setCreateTime(new Date());
            messageService.sendMessage(msg);
        }
    }

    @Override
    @Transactional
    public void deleteRequirement(Integer id) {
        Requirement req = requirementMapper.findById(id);
        if (req != null) {
            projectUtils.checkNotArchived(req.getProjectId());
        }
        requirementMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Integer id : ids) {
            requirementMapper.deleteById(id);
        }
    }

    // 私有消息发送方法
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