package com.example.smarttestplatform.module.apitester.service;

import com.example.smarttestplatform.module.apitester.entity.ApiInfo;
import java.util.List;

public interface ApiInfoService {
    List<ApiInfo> getApisByModule(Integer moduleId);
    List<ApiInfo> getApisByProject(Integer projectId);   // 新增
    ApiInfo getApiById(Integer id);
    void createApi(ApiInfo apiInfo, Integer userId);
    void updateApi(ApiInfo apiInfo);
    void deleteApi(Integer id);
}