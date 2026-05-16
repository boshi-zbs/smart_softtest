package com.example.smarttestplatform.module.operationlog.entity;

import lombok.Data;
import java.util.Date;

@Data
public class OperationLog {
    private Integer id;
    private Integer userId;
    private String username;
    private String module;
    private String operation;
    private String description;
    private Integer targetId;
    private String requestParams;
    private String result;
    private String ipAddress;
    private String userAgent;
    private Date createTime;
}