package com.example.smarttestplatform.module.apitester.dto;

import lombok.Data;
import java.util.List;

@Data
public class AIGeneratedApi {
    private String name;
    private String method;
    private String url;
    private String headers;
    private String description;
    private List<AIGeneratedTestCase> testCases;
}