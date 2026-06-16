package com.example.smarttestplatform.module.apitester.service.impl;

import com.example.smarttestplatform.common.utils.ProjectUtils;
import com.example.smarttestplatform.module.apitester.entity.ApiModule;
import com.example.smarttestplatform.module.apitester.mapper.ApiModuleMapper;
import com.example.smarttestplatform.module.apitester.service.ApiModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApiModuleServiceImpl implements ApiModuleService {

    @Autowired
    private ApiModuleMapper moduleMapper;
    @Autowired
    private ProjectUtils projectUtils;

    @Override
    public List<ApiModule> getModulesByProject(Integer projectId) {
        return moduleMapper.findByProjectId(projectId);
    }

    @Override
    @Transactional
    public void addModule(ApiModule module) {
        projectUtils.checkNotArchived(module.getProjectId());
        moduleMapper.insert(module);
    }

    @Override
    @Transactional
    public void updateModule(ApiModule module) {
        ApiModule old = moduleMapper.findById(module.getId());
        if (old != null) projectUtils.checkNotArchived(old.getProjectId());
        moduleMapper.update(module);
    }

    @Override
    @Transactional
    public void deleteModule(Integer id) {
        ApiModule module = moduleMapper.findById(id);
        if (module != null) projectUtils.checkNotArchived(module.getProjectId());
        moduleMapper.deleteById(id);
    }
}