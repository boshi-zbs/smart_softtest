package com.example.smarttestplatform.module.testexecution.entity;

import lombok.Data;
import java.util.Date;

@Data
public class TestExecutionAttachment {
    private Integer id;
    private Integer executionId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private Date uploadTime;
}