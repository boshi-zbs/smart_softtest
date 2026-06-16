package com.example.smarttestplatform.module.apitester.service;

import com.example.smarttestplatform.module.apitester.entity.ApiModule;
import java.util.List;

public interface ApiModuleService {
    List<ApiModule> getModulesByProject(Integer projectId);
    void addModule(ApiModule module);
    void updateModule(ApiModule module);
    void deleteModule(Integer id);
}