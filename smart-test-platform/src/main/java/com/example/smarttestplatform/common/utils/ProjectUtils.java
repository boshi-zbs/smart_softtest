package com.example.smarttestplatform.common.utils;

import com.example.smarttestplatform.module.project.entity.Project;
import com.example.smarttestplatform.module.project.mapper.ProjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProjectUtils {

    @Autowired
    private ProjectMapper projectMapper;

    /**
     * 校验项目是否已归档（status = 0）
     * @param projectId 项目ID
     * @throws RuntimeException 如果项目已归档
     */
    public void checkNotArchived(Integer projectId) {
        if (projectId == null) return;
        Project project = projectMapper.findById(projectId);
        if (project != null && project.getStatus() == 0) {
            throw new RuntimeException("项目已归档，不允许进行此操作");
        }
    }
}