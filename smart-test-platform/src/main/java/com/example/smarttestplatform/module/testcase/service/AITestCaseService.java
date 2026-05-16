package com.example.smarttestplatform.module.testcase.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface AITestCaseService {
    List<Map<String, Object>> generateTestCases(MultipartFile file, String content, Integer projectId);
}