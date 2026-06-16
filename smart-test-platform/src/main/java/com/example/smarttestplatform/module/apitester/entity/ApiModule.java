package com.example.smarttestplatform.module.apitester.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ApiModule {
    private Integer id;
    private Integer projectId;
    private String moduleName;
    private Integer parentId;
    private String description;
    private Date createTime;
}