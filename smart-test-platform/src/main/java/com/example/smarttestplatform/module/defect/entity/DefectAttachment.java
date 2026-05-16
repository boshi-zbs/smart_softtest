package com.example.smarttestplatform.module.defect.entity;

import lombok.Data;
import java.util.Date;

@Data
public class DefectAttachment {
    private Integer id;
    private Integer defectId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private Date uploadTime;
}