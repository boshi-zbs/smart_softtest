package com.example.smarttestplatform.module.apitester.dto;

import lombok.Data;
import java.util.List;

@Data
public class AIGeneratedModule {
    private String moduleName;
    private String description;
    private List<AIGeneratedApi> apis;
}