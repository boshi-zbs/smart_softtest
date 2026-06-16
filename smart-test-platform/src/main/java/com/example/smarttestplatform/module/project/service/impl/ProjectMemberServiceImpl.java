package com.example.smarttestplatform.module.project.service.impl;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.utils.ProjectUtils;
import com.example.smarttestplatform.module.message.entity.Message;
import com.example.smarttestplatform.module.message.service.MessageService;
import com.example.smarttestplatform.module.project.entity.Project;
import com.example.smarttestplatform.module.project.entity.ProjectMember;
import com.example.smarttestplatform.module.project.mapper.ProjectMemberMapper;
import com.example.smarttestplatform.module.project.mapper.ProjectMapper;
import com.example.smarttestplatform.module.project.service.ProjectMemberService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class ProjectMemberServiceImpl implements ProjectMemberService {

    @Autowired
    private ProjectMemberMapper projectMemberMapper;
    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private MessageService messageService;
    @Autowired
    private ProjectUtils projectUtils;


    @Override
    public List<ProjectMember> findByProjectId(Integer projectId) {
        return projectMemberMapper.findByProjectId(projectId);
    }

    @Override
    public PageResult<ProjectMember> pageQuery(PageRequest pageRequest) {
        Map<String, Object> conditions = pageRequest.getConditions();
        int offset = (pageRequest.getPage() - 1) * pageRequest.getSize();
        List<ProjectMember> records = projectMemberMapper.pageQuery(conditions, offset, pageRequest.getSize());
        for (ProjectMember member : records) {
            enrichProjectMember(member);
        }
        Long total = projectMemberMapper.count(conditions);
        return new PageResult<>(records, total, pageRequest.getPage(), pageRequest.getSize());
    }

    private void enrichProjectMember(ProjectMember member) {
        if (member.getProjectId() != null) {
            Project project = projectMapper.findById(member.getProjectId());
            if (project != null) {
                member.setProjectName(project.getProjectName());
            }
        }
        if (member.getUserId() != null) {
            User user = userMapper.findById(member.getUserId());
            if (user != null) {
                member.setUserName(user.getUsername());
                member.setUserRealName(user.getRealName());
            }
        }
    }

    @Override
    @Transactional
    public void addMember(ProjectMember projectMember, Integer operatorId) {
        projectUtils.checkNotArchived(projectMember.getProjectId());

        ProjectMember existing = projectMemberMapper.findByProjectAndUser(projectMember.getProjectId(), projectMember.getUserId());
        if (existing != null) {
            throw new RuntimeException("该成员已存在于项目中");
        }
        projectMemberMapper.insert(projectMember);

        Project project = projectMapper.findById(projectMember.getProjectId());
        if (project != null) {
            String title = "加入项目";
            String content = "您已被添加至项目【" + project.getProjectName() + "】，角色：" + projectMember.getRoleInProject();
            String type = "通知";
            String status = null;
            if ("开发人员".equals(projectMember.getRoleInProject())) {
                type = "待办";
                status = "pending";
            } else if ("测试人员".equals(projectMember.getRoleInProject())) {
                type = "待办";
                status = "pending";
            }

            Message msg = new Message();
            msg.setSenderId(operatorId);
            msg.setReceiverId(projectMember.getUserId());
            msg.setTitle(title);
            msg.setContent(content);
            msg.setType(type);
            msg.setRelatedId(project.getId());
            msg.setRelatedType("project");
            msg.setIsRead(false);
            msg.setStatus(status);
            msg.setCreateTime(new Date());
            messageService.sendMessage(msg);
        }
    }

    @Override
    @Transactional
    public void updateMember(ProjectMember projectMember) {
        // 1. 通过联合主键 (projectId, userId) 查询旧记录
        ProjectMember old = projectMemberMapper.findByProjectAndUser(
                projectMember.getProjectId(),
                projectMember.getUserId()
        );
        if (old == null) {
            throw new RuntimeException("成员记录不存在");
        }
        // 2. 校验项目是否归档（使用旧记录中的 projectId）
        projectUtils.checkNotArchived(old.getProjectId());
        // 3. 更新角色（只更新 roleInProject 字段，Mapper 中的 update 语句基于联合主键）
        projectMemberMapper.update(projectMember);
    }

    @Override
    @Transactional
    public void removeMember(Integer projectId, Integer userId) {
        // 先查询是否存在
        ProjectMember member = projectMemberMapper.findByProjectAndUser(projectId, userId);
        if (member == null) {
            throw new RuntimeException("成员不存在");
        }
        projectUtils.checkNotArchived(member.getProjectId());
        projectMemberMapper.deleteByProjectAndUser(projectId, userId);
    }

    @Override
    @Transactional
    public void removeBatch(List<ProjectMember> members) {
        if (members == null || members.isEmpty()) return;
        for (ProjectMember member : members) {
            // 先检查是否存在
            ProjectMember existing = projectMemberMapper.findByProjectAndUser(member.getProjectId(), member.getUserId());
            if (existing == null) continue;
            projectUtils.checkNotArchived(existing.getProjectId());
            projectMemberMapper.deleteByProjectAndUser(member.getProjectId(), member.getUserId());
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
}