package com.example.smarttestplatform.common.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class PageRequest {
    private Integer page = 1;
    private Integer size = 10;
    private Map<String, Object> conditions = new HashMap<>();
}