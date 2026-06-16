package com.example.smarttestplatform.module.apitester.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AIDocGenerateRequest {
    private MultipartFile file;
    private String content;
    private Integer projectId;
}