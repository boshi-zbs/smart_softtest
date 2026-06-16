package com.example.smarttestplatform.module.apitester.service.impl;

import com.example.smarttestplatform.common.utils.ProjectUtils;
import com.example.smarttestplatform.module.apitester.entity.ApiInfo;
import com.example.smarttestplatform.module.apitester.mapper.ApiInfoMapper;
import com.example.smarttestplatform.module.apitester.service.ApiInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApiInfoServiceImpl implements ApiInfoService {

    @Autowired
    private ApiInfoMapper apiInfoMapper;
    @Autowired
    private ProjectUtils projectUtils;

    @Override
    public List<ApiInfo> getApisByModule(Integer moduleId) {
        return apiInfoMapper.listByModule(moduleId, null);
    }

    @Override
    public List<ApiInfo> getApisByProject(Integer projectId) {
        return apiInfoMapper.listByModule(null, projectId);
    }

    @Override
    public ApiInfo getApiById(Integer id) {
        return apiInfoMapper.findById(id);
    }

    @Override
    @Transactional
    public void createApi(ApiInfo apiInfo, Integer userId) {
        projectUtils.checkNotArchived(apiInfo.getProjectId());
        apiInfo.setCreateUserId(userId);
        apiInfoMapper.insert(apiInfo);
    }

    @Override
    @Transactional
    public void updateApi(ApiInfo apiInfo) {
        ApiInfo old = apiInfoMapper.findById(apiInfo.getId());
        if (old != null) projectUtils.checkNotArchived(old.getProjectId());
        apiInfoMapper.update(apiInfo);
    }

    @Override
    @Transactional
    public void deleteApi(Integer id) {
        ApiInfo api = apiInfoMapper.findById(id);
        if (api != null) projectUtils.checkNotArchived(api.getProjectId());
        apiInfoMapper.deleteById(id);
    }
}