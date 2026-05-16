package com.example.smarttestplatform.module.autotest.entity;

import lombok.Data;

@Data
public class AutoTestStep {
    private Integer id;
    private Integer caseId;
    private Integer stepOrder;
    private String actionType;        // 对应数据库 action_type
    private String locatorType;
    private String locatorValue;
    private String inputValue;
    private Integer waitSeconds;
    private String description;
}