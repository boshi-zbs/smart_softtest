package com.example.smarttestplatform.module.apitester.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ApiTestExecution {
    private Integer id;
    private Integer caseId;
    private Integer executorId;
    private String requestUrl;
    private String requestMethod;
    private String requestHeaders;
    private String requestBody;
    private Integer responseStatus;
    private String responseBody;
    private Boolean assertResult;
    private String assertMessage;
    private Integer durationMs;
    private Date executeTime;

}