package com.example.smarttestplatform.module.autotest.entity;

import java.util.List;
import lombok.Data;

@Data
// 请求体封装类
public class AutoTestCaseRequest {
    private AutoTestCase testCase;
    private List<AutoTestStep> steps;
    // getters/setters
}
