package com.example.smarttestplatform.module.mytodo.dto;

import lombok.Data;

@Data
public class TodoItemDTO {
    private Integer id;
    private String type;          // requirement / defect
    private String title;
    private Integer projectId;
    private String projectName;
    private String status;
    private String priority;
    private Integer assigneeId;
    private String assigneeName;
    private String createTime;
}