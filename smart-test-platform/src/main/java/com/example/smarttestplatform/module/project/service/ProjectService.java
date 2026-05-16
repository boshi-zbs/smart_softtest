package com.example.smarttestplatform.module.project.service;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.project.entity.Project;

import java.util.List;

public interface ProjectService {
    Project findById(Integer id);
    Project findByKey(String projectKey);
    PageResult<Project> pageQuery(PageRequest pageRequest);
    void createProject(Project project, Integer createUserId);
    void updateProject(Project project);
    void deleteProject(Integer id);
    void deleteBatch(List<Integer> ids);
}