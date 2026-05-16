package com.example.smarttestplatform.module.testcase.dto;

import com.example.smarttestplatform.module.testcase.entity.TestCase;
import lombok.Data;
import java.util.List;

@Data
public class BatchCreateRequest {
    private List<TestCase> cases;
    private Integer planId;
}