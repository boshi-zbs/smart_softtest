package com.example.smarttestplatform.module.projectmodule.service.impl;

import com.example.smarttestplatform.common.utils.ProjectUtils;
import com.example.smarttestplatform.module.project.entity.Project;
import com.example.smarttestplatform.module.project.mapper.ProjectMapper;
import com.example.smarttestplatform.module.projectmodule.entity.ProjectModule;
import com.example.smarttestplatform.module.projectmodule.mapper.ProjectModuleMapper;
import com.example.smarttestplatform.module.projectmodule.service.ProjectModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectModuleServiceImpl implements ProjectModuleService {

    @Autowired
    private ProjectModuleMapper projectModuleMapper;
    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private ProjectUtils projectUtils;

    @Override
    public ProjectModule findById(Integer id) {
        return projectModuleMapper.findById(id);
    }

    @Override
    public List<ProjectModule> findByProjectId(Integer projectId) {
        List<ProjectModule> list = projectModuleMapper.findByProjectId(projectId);
        Project project = projectMapper.findById(projectId);
        if (project != null) {
            for (ProjectModule pm : list) {
                pm.setProjectName(project.getProjectName());
            }
        }
        return list;
    }

    @Override
    @Transactional
    public void createModule(ProjectModule module) {
        projectUtils.checkNotArchived(module.getProjectId());
        // 检查同一项目下模块名是否重复
        List<ProjectModule> existing = projectModuleMapper.findByProjectId(module.getProjectId());
        if (existing.stream().anyMatch(m -> m.getModuleName().equals(module.getModuleName()))) {
            throw new RuntimeException("模块名称已存在");
        }
        projectModuleMapper.insert(module);
    }

    @Override
    @Transactional
    public void updateModule(ProjectModule module) {
        ProjectModule old = projectModuleMapper.findById(module.getId());
        if (old != null) {
            projectUtils.checkNotArchived(old.getProjectId());
        }
        if (old == null) throw new RuntimeException("模块不存在");
        // 如果修改了模块名，检查是否重复
        if (!old.getModuleName().equals(module.getModuleName())) {
            List<ProjectModule> existing = projectModuleMapper.findByProjectId(module.getProjectId());
            if (existing.stream().anyMatch(m -> m.getModuleName().equals(module.getModuleName()))) {
                throw new RuntimeException("模块名称已存在");
            }
        }
        projectModuleMapper.update(module);
    }

    @Override
    @Transactional
    public void deleteModule(Integer id) {
        ProjectModule module = projectModuleMapper.findById(id);
        if (module != null) {
            projectUtils.checkNotArchived(module.getProjectId());
        }
        projectModuleMapper.deleteById(id);
    }
}