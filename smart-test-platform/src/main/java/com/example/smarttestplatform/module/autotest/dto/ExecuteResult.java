package com.example.smarttestplatform.module.autotest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExecuteResult {
    private boolean success;
    private String message;
    private String screenshotUrl;  // 新增
}