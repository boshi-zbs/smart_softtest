package com.example.smarttestplatform.module.testcase.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AIGenerateRequest {
    private String content;          // 文本内容
    private Integer projectId;       // 项目ID
    private MultipartFile file;      // 上传的文件
}