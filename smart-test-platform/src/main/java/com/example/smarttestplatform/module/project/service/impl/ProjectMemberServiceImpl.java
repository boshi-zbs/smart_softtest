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
    public ProjectMember findById(Integer id) {
        return projectMemberMapper.findById(id);
    }

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
        ProjectMember old = projectMemberMapper.findById(projectMember.getId());
        if (old != null) {
            projectUtils.checkNotArchived(old.getProjectId());
        }
        // 更新时一般只修改角色，不需要发送消息（可选）
        projectMemberMapper.update(projectMember);
    }

    @Override
    @Transactional
    public void removeMember(Integer id) {
        ProjectMember member = projectMemberMapper.findById(id);
        if (member != null) {
            projectUtils.checkNotArchived(member.getProjectId());
        }

        if (member != null) {
            projectMemberMapper.deleteById(id);
            // 可以发送移除通知（可选）
        }
    }

    @Override
    @Transactional
    public void removeBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Integer id : ids) {
            projectMemberMapper.deleteById(id);
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