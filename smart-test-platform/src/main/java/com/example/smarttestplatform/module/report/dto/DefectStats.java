package com.example.smarttestplatform.module.report.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DefectStats {
    private Map<String, Integer> byStatus;       // 按状态
    private Map<String, Integer> bySeverity;     // 按严重程度
    private Map<String, Integer> byPriority;     // 按优先级
    private List<DefectTrendItem> trend;         // 趋势
}

