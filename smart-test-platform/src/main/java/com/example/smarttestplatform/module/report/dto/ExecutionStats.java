package com.example.smarttestplatform.module.report.dto;

import lombok.Data;
import java.util.List;

@Data
public class ExecutionStats {
    private Integer totalCases;          // 计划总用例数
    private Integer executedCases;        // 已执行用例数
    private Integer passed;               // 通过
    private Integer failed;               // 失败
    private Integer blocked;              // 阻塞
    private Integer skipped;              // 跳过
    private Double progress;              // 进度百分比
    private List<ExecutionTrendItem> trend; // 执行趋势（按天）
}

