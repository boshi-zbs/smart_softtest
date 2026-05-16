package com.example.smarttestplatform.module.testcase.service;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.testcase.entity.TestCase;

import java.util.List;

public interface TestCaseService {
    TestCase findById(Integer id);
    PageResult<TestCase> pageQuery(PageRequest pageRequest);
    void createTestCase(TestCase testCase, Integer creatorId);
    void updateTestCase(TestCase testCase);
    void deleteTestCase(Integer id);
    void deleteBatch(java.util.List<Integer> ids);

    void batchCreate(List<TestCase> cases, Integer planId, Integer creatorId);
}