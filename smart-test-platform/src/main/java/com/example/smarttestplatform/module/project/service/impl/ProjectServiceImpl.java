package com.example.smarttestplatform.module.project.service.impl;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.project.entity.Project;
import com.example.smarttestplatform.module.project.mapper.ProjectMapper;
import com.example.smarttestplatform.module.project.service.ProjectService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ProjectServiceImpl implements ProjectService {

    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Project findById(Integer id) {
        return projectMapper.findById(id);
    }

    @Override
    public Project findByKey(String projectKey) {
        return projectMapper.findByKey(projectKey);
    }

    @Override
    public PageResult<Project> pageQuery(PageRequest pageRequest) {
        Map<String, Object> conditions = pageRequest.getConditions();
        int offset = (pageRequest.getPage() - 1) * pageRequest.getSize();
        List<Project> records = projectMapper.pageQuery(conditions, offset, pageRequest.getSize());
        // 填充创建人姓名
        for (Project project : records) {
            if (project.getCreateUserId() != null) {
                User user = userMapper.findById(project.getCreateUserId());
                if (user != null) {
                    project.setCreateUserName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                }
            }
        }
        Long total = projectMapper.count(conditions);
        return new PageResult<>(records, total, pageRequest.getPage(), pageRequest.getSize());
    }

    @Override
    @Transactional
    public void createProject(Project project, Integer createUserId) {
        // 检查项目标识是否已存在
        if (projectMapper.findByKey(project.getProjectKey()) != null) {
            throw new RuntimeException("项目标识已存在");
        }
        project.setCreateUserId(createUserId);
        projectMapper.insert(project);
    }

    @Override
    @Transactional
    public void updateProject(Project project) {
        // 检查项目标识是否被其他项目使用
        Project existing = projectMapper.findByKey(project.getProjectKey());
        if (existing != null && !existing.getId().equals(project.getId())) {
            throw new RuntimeException("项目标识已存在");
        }
        projectMapper.update(project);
    }

    @Override
    @Transactional
    public void deleteProject(Integer id) {
        projectMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteBatch(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Integer id : ids) {
            projectMapper.deleteById(id);
        }
        // 或者使用批量删除 SQL，但简单循环也可
    }
}