package com.example.smarttestplatform.module.ai.entity;

import lombok.Data;
import java.util.Date;

@Data
public class AIChatHistory {
    private Integer id;
    private String sessionId;
    private Integer defectId;
    private String role;
    private String content;
    private Date createTime;
}