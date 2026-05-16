package com.example.smarttestplatform.module.projectmodule.service;

import com.example.smarttestplatform.module.projectmodule.entity.ProjectModule;
import java.util.List;

public interface ProjectModuleService {
    ProjectModule findById(Integer id);
    List<ProjectModule> findByProjectId(Integer projectId);
    void createModule(ProjectModule module);
    void updateModule(ProjectModule module);
    void deleteModule(Integer id);
}