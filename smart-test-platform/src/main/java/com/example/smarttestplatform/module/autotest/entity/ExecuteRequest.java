package com.example.smarttestplatform.module.autotest.entity;
import lombok.Data;

@Data
public class ExecuteRequest {
    private String baseUrl;
    private Boolean headless;  // 新增：是否无头模式，默认 true
    // 添加辅助方法，安全获取 headless 值
    public boolean getHeadlessOrDefault() {
        return headless != null ? headless : true;
    }
}
