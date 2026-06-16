package com.example.smarttestplatform.module.apitester.dto;

import lombok.Data;

@Data
public class AIGeneratedTestCase {
    private String caseName;
    private String requestParams;
    private Integer expectedStatus;
    private String expectedResponse;
}