package com.example.smarttestplatform.module.report.dto;

import lombok.Data;

@Data
public class ExecutionTrendItem {
    private String date;
    private int passed;
    private int failed;
    private int blocked;
}