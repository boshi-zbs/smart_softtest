package com.example.smarttestplatform.module.defect.entity;

import lombok.Data;
import java.util.Date;

@Data
public class DefectComment {
    private Integer id;
    private Integer defectId;
    private Integer userId;
    private String action;        // 评论、状态变更、指派等
    private String oldValue;
    private String newValue;
    private String content;
    private Date createTime;

    // 非数据库字段
    private String userName;
}