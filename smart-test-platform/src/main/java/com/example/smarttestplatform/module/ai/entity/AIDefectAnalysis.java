package com.example.smarttestplatform.module.ai.entity;

import lombok.Data;
import java.util.Date;

@Data
public class AIDefectAnalysis {
    private Integer id;
    private Integer defectId;
    private Integer projectId;
    private String analysisType;
    private String analysisResult;
    private String fixSuggestions;
    private String affectedFiles;
    private String status;
    private Integer createdBy;
    private Date createTime;
    private Date updateTime;
}