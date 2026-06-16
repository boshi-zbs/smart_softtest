package com.example.smarttestplatform.module.apitester.controller;

import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.apitester.dto.AIDocGenerateRequest;
import com.example.smarttestplatform.module.apitester.entity.*;
import com.example.smarttestplatform.module.apitester.service.*;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.service.UserService;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/api-tester")
@PreAuthorize("hasAnyRole('ADMIN', 'TESTER')")
public class ApiTesterController {

    @Autowired
    private ApiModuleService moduleService;
    @Autowired
    private ApiInfoService apiInfoService;
    @Autowired
    private ApiTestCaseService testCaseService;
    @Autowired
    private UserService userService;

    // ========== 模块管理 ==========
    @GetMapping("/modules")
    public Result<List<ApiModule>> getModules(@RequestParam Integer projectId) {
        return Result.success(moduleService.getModulesByProject(projectId));
    }
    @DeleteMapping("/executions/batch-delete")
    public Result<String> batchDeleteExecutions(@RequestBody List<Integer> ids) {
        testCaseService.batchDeleteExecutions(ids);
        return Result.success("删除成功");
    }
    @PostMapping("/module")
    public Result<String> addModule(@RequestBody ApiModule module) {
        moduleService.addModule(module);
        return Result.success("添加成功");
    }

    @PutMapping("/module")
    public Result<String> updateModule(@RequestBody ApiModule module) {
        moduleService.updateModule(module);
        return Result.success("更新成功");
    }

    @DeleteMapping("/module/{id}")
    public Result<String> deleteModule(@PathVariable Integer id) {
        moduleService.deleteModule(id);
        return Result.success("删除成功");
    }

    // ========== 接口管理 ==========
    @GetMapping("/apis")
    public Result<List<ApiInfo>> getApis(@RequestParam(required = false) Integer moduleId,
                                         @RequestParam(required = false) Integer projectId) {
        if (moduleId != null) {
            return Result.success(apiInfoService.getApisByModule(moduleId));
        } else {
            return Result.success(apiInfoService.getApisByProject(projectId));
        }
    }

    @GetMapping("/api/{id}")
    public Result<ApiInfo> getApi(@PathVariable Integer id) {
        return Result.success(apiInfoService.getApiById(id));
    }

    @PostMapping("/api")
    public Result<String> addApi(@RequestBody ApiInfo apiInfo,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        apiInfoService.createApi(apiInfo, user.getId());
        return Result.success("添加成功");
    }

    @PutMapping("/api")
    public Result<String> updateApi(@RequestBody ApiInfo apiInfo) {
        apiInfoService.updateApi(apiInfo);
        return Result.success("更新成功");
    }

    @DeleteMapping("/api/{id}")
    public Result<String> deleteApi(@PathVariable Integer id) {
        apiInfoService.deleteApi(id);
        return Result.success("删除成功");
    }

    // ========== 测试用例管理 ==========
    @GetMapping("/test-cases")
    public Result<List<ApiTestCase>> getTestCases(@RequestParam Integer apiId) {
        return Result.success(testCaseService.getTestCasesByApi(apiId));
    }

    @PostMapping("/test-case")
    public Result<String> addTestCase(@RequestBody ApiTestCase testCase,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        testCaseService.createTestCase(testCase, user.getId());
        return Result.success("添加成功");
    }

    @PutMapping("/test-case")
    public Result<String> updateTestCase(@RequestBody ApiTestCase testCase) {
        testCaseService.updateTestCase(testCase);
        return Result.success("更新成功");
    }

    @DeleteMapping("/test-case/{id}")
    public Result<String> deleteTestCase(@PathVariable Integer id) {
        testCaseService.deleteTestCase(id);
        return Result.success("删除成功");
    }

    @PostMapping("/test-case/{caseId}/execute")
    public Result<ApiTestExecution> executeTestCase(@PathVariable Integer caseId,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        ApiTestExecution execution = testCaseService.executeTestCase(caseId, user.getId());
        return Result.success(execution);
    }

    @PostMapping("/test-cases/batch-execute")
    public Result<List<ApiTestExecution>> batchExecute(@RequestBody List<Integer> caseIds,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        List<ApiTestExecution> executions = testCaseService.batchExecute(caseIds, user.getId());
        return Result.success(executions);
    }

    @DeleteMapping("/test-cases/batch-delete")
    public Result<String> batchDelete(@RequestBody List<Integer> ids) {
        testCaseService.batchDelete(ids);
        return Result.success("批量删除成功");
    }

    @GetMapping("/test-case/{caseId}/last-failed-execution")
    public Result<ApiTestExecution> getLastFailedExecution(@PathVariable Integer caseId) {
        ApiTestExecution execution = testCaseService.getLastFailedExecution(caseId);
        return Result.success(execution);
    }

    @GetMapping("/executions/{caseId}")
    public Result<List<ApiTestExecution>> getExecutions(@PathVariable Integer caseId) {
        return Result.success(testCaseService.getExecutions(caseId));
    }

    // ========== AI 生成（根据文档） ==========
    @PostMapping("/ai-generate-from-doc")
    public Result<List<ApiModule>> generateFromDoc(AIDocGenerateRequest request,
                                                   @AuthenticationPrincipal UserDetails userDetails) throws IOException {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) return Result.error("用户不存在");
        String docText = request.getContent();
        if (request.getFile() != null && !request.getFile().isEmpty()) {
            // 新增：根据文件类型提取文本
            docText = extractTextFromFile(request.getFile());
        }
        if (docText == null || docText.trim().isEmpty()) {
            return Result.error("请提供文档内容或上传文件");
        }
        List<ApiModule> modules = testCaseService.generateModulesAndApisFromDoc(request.getProjectId(), docText);
        return Result.success(modules);
    }

    private String extractTextFromFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) return "";
        String lower = originalFilename.toLowerCase();
        if (lower.endsWith(".txt") || lower.endsWith(".md")) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } else if (lower.endsWith(".docx")) {
            try (InputStream is = file.getInputStream();
                 XWPFDocument doc = new XWPFDocument(is)) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph p : doc.getParagraphs()) {
                    String text = p.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        sb.append(text).append("\n");
                    }
                }
                return sb.toString();
            }
        } else {
            throw new RuntimeException("不支持的文件类型，仅支持 .txt, .md, .docx");
        }
    }
    @GetMapping("/env/{projectId}")
    public Result<Map<String, String>> getEnv(@PathVariable Integer projectId) {
        return Result.success(testCaseService.getEnvVariables(projectId));
    }

    @PostMapping("/env")
    public Result<String> saveEnv(@RequestBody Map<String, Object> body) {
        Integer projectId = (Integer) body.get("projectId");
        Map<String, String> variables = (Map<String, String>) body.get("variables");
        testCaseService.saveEnvVariables(projectId, variables);
        return Result.success("保存成功");
    }
}