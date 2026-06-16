package com.example.smarttestplatform.module.apitester.service;

import com.example.smarttestplatform.module.apitester.entity.ApiModule;
import com.example.smarttestplatform.module.apitester.entity.ApiTestCase;
import com.example.smarttestplatform.module.apitester.entity.ApiTestExecution;
import java.util.List;
import java.util.Map;

public interface ApiTestCaseService {
    List<ApiTestCase> getTestCasesByApi(Integer apiId);
    void createTestCase(ApiTestCase testCase, Integer userId);
    void updateTestCase(ApiTestCase testCase);
    void deleteTestCase(Integer id);
    ApiTestExecution executeTestCase(Integer caseId, Integer executorId);
    List<ApiTestExecution> getExecutions(Integer caseId);



    List<ApiModule> generateModulesAndApisFromDoc(Integer projectId, String docText);
    void batchDelete(List<Integer> ids);
    List<ApiTestExecution> batchExecute(List<Integer> caseIds, Integer executorId);
    ApiTestExecution getLastFailedExecution(Integer caseId);

    Map<String, String> getEnvVariables(Integer projectId);
    void saveEnvVariables(Integer projectId, Map<String, String> variables);

    void batchDeleteExecutions(List<Integer> ids);

}