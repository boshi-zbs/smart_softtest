package com.example.smarttestplatform.module.apitester.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ApiTestCase {
    private Integer id;
    private Integer apiId;
    private String caseName;
    private String requestParams; // JSON
    private Integer expectedStatus;
    private String expectedResponse;
    private String assertType;
    private String description;
    private Boolean isGenerated;
    private Integer createUserId;
    private Date createTime;
    private Date updateTime;

    // 非DB
    private String apiName;
    private String apiUrl;
    private String apiMethod;
    private String lastExecutionResult;  // "成功"/"失败"/"未执行"
    private Date lastExecutionTime;
}