package com.example.smarttestplatform.module.apitester.service.impl;

import com.example.smarttestplatform.common.utils.ProjectUtils;
import com.example.smarttestplatform.module.apitester.dto.*;
import com.example.smarttestplatform.module.apitester.entity.*;
import com.example.smarttestplatform.module.apitester.mapper.*;
import com.example.smarttestplatform.module.apitester.service.ApiTestCaseService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.mapper.UserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ApiTestCaseServiceImpl implements ApiTestCaseService {

    @Autowired
    private ApiTestCaseMapper testCaseMapper;
    @Autowired
    private ApiInfoMapper apiInfoMapper;
    @Autowired
    private ApiTestExecutionMapper executionMapper;
    @Autowired
    private ApiModuleMapper moduleMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ProjectUtils projectUtils;
    @Autowired
    private ChatClient chatClient;
    @Autowired
    private ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();
    @Autowired
    private ApiProjectEnvMapper envMapper;
    private static final Logger log = LoggerFactory.getLogger(ApiTestCaseServiceImpl.class);

    // ================= 基础 CRUD =================
    @Override
    public List<ApiTestCase> getTestCasesByApi(Integer apiId) {
        return testCaseMapper.findByApiIdWithLastResult(apiId);
    }

    @Override
    @Transactional
    public void createTestCase(ApiTestCase testCase, Integer userId) {
        ApiInfo api = apiInfoMapper.findById(testCase.getApiId());
        if (api == null) throw new RuntimeException("接口不存在");
        projectUtils.checkNotArchived(api.getProjectId());
        testCase.setCreateUserId(userId);
        testCase.setIsGenerated(false);
        testCaseMapper.insert(testCase);
    }

    @Override
    @Transactional
    public void updateTestCase(ApiTestCase testCase) {
        ApiTestCase old = testCaseMapper.findById(testCase.getId());
        if (old == null) throw new RuntimeException("用例不存在");
        ApiInfo api = apiInfoMapper.findById(old.getApiId());
        if (api != null) projectUtils.checkNotArchived(api.getProjectId());
        testCaseMapper.update(testCase);
    }

    @Override
    @Transactional
    public void deleteTestCase(Integer id) {
        ApiTestCase testCase = testCaseMapper.findById(id);
        if (testCase == null) return;
        ApiInfo api = apiInfoMapper.findById(testCase.getApiId());
        if (api != null) projectUtils.checkNotArchived(api.getProjectId());
        testCaseMapper.deleteById(id);
    }

    // ================= 批量操作 =================
    @Override
    @Transactional
    public void batchDelete(List<Integer> ids) {
        for (Integer id : ids) {
            deleteTestCase(id);
        }
    }

    @Override
    public List<ApiTestExecution> batchExecute(List<Integer> caseIds, Integer executorId) {
        List<ApiTestExecution> executions = new ArrayList<>();
        for (Integer caseId : caseIds) {
            executions.add(executeTestCase(caseId, executorId));
        }
        return executions;
    }

    // ================= 执行用例 =================
    @Override
    public ApiTestExecution executeTestCase(Integer caseId, Integer executorId) {
        ApiTestCase testCase = testCaseMapper.findById(caseId);
        if (testCase == null) throw new RuntimeException("用例不存在");
        ApiInfo api = apiInfoMapper.findById(testCase.getApiId());
        if (api == null) throw new RuntimeException("关联接口不存在");

        Map<String, String> env = getEnvVariables(api.getProjectId());

        String rawUrl = api.getUrl();
        String finalUrl = replaceVariables(rawUrl, env);
        if (!finalUrl.matches("^https?://.*")) {
            String baseUrl = env.getOrDefault("base_url", "");
            if (baseUrl.isEmpty()) {
                throw new RuntimeException("未配置 base_url 环境变量，且 URL 不是完整地址");
            }
            finalUrl = (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl) +
                    (finalUrl.startsWith("/") ? finalUrl : "/" + finalUrl);
        }

        // 请求头处理：只支持 key:value,key:value 格式（前端已转换）
        String rawHeaders = api.getHeaders();
        HttpHeaders headers = new HttpHeaders();
        if (rawHeaders != null && !rawHeaders.trim().isEmpty()) {
            String processedHeaders = replaceVariables(rawHeaders, env);
            Map<String, String> headerMap = parseKeyValueHeaders(processedHeaders);
            if (headerMap != null && !headerMap.isEmpty()) {
                headerMap.forEach((key, value) -> {
                    if ("Authorization".equalsIgnoreCase(key)) {
                        value = value.trim().replaceAll("\\s+", " ");
                        log.info("最终 Authorization 头值: {}", value);
                    }
                    headers.set(key, value);
                });
            } else {
                log.error("无法解析请求头，原始内容: {}", rawHeaders);
            }
        }

        String requestBody = null;
        if (testCase.getRequestParams() != null && !testCase.getRequestParams().isEmpty()) {
            requestBody = testCase.getRequestParams();
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        long start = System.currentTimeMillis();
        ApiTestExecution execution = new ApiTestExecution();
        execution.setCaseId(caseId);
        execution.setExecutorId(executorId);
        execution.setRequestUrl(finalUrl);
        execution.setRequestMethod(api.getMethod());
        execution.setRequestHeaders(rawHeaders);
        execution.setRequestBody(requestBody);

        try {
            HttpMethod method = HttpMethod.valueOf(api.getMethod());
            ResponseEntity<String> response = restTemplate.exchange(finalUrl, method, entity, String.class);
            int status = response.getStatusCodeValue();
            String body = response.getBody();
            execution.setResponseStatus(status);
            execution.setResponseBody(body);

            boolean assertSuccess = false;
            String assertMsg = "";
            Integer expectedCode = testCase.getExpectedStatus();
            if (expectedCode != null) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(body);
                    int actualCode = jsonNode.has("code") ? jsonNode.get("code").asInt() : -1;
                    if (actualCode == expectedCode) {
                        assertSuccess = true;
                    } else {
                        assertMsg = "业务状态码不匹配，期望:" + expectedCode + "，实际:" + actualCode;
                    }
                } catch (Exception e) {
                    assertMsg = "响应体JSON解析失败或缺少code字段: " + e.getMessage();
                }
            } else if (testCase.getExpectedResponse() != null && !testCase.getExpectedResponse().isEmpty()) {
                String expected = testCase.getExpectedResponse();
                if ("contains".equals(testCase.getAssertType())) {
                    if (body != null && body.contains(expected)) {
                        assertSuccess = true;
                    } else {
                        assertMsg = "响应体中未包含预期内容: " + expected;
                    }
                } else {
                    assertSuccess = true;
                }
            } else {
                assertSuccess = true;
            }
            execution.setAssertResult(assertSuccess);
            execution.setAssertMessage(assertMsg);
        } catch (Exception e) {
            execution.setAssertResult(false);
            execution.setAssertMessage("请求异常: " + e.getMessage());
            execution.setResponseStatus(0);
        }
        execution.setDurationMs((int) (System.currentTimeMillis() - start));
        executionMapper.insert(execution);
        return execution;
    }

    /**
     * 只解析 key:value,key2:value2 格式的请求头
     */
    private Map<String, String> parseKeyValueHeaders(String headers) {
        if (headers == null || headers.trim().isEmpty()) {
            return new HashMap<>();
        }
        Map<String, String> result = new HashMap<>();
        String[] pairs = headers.split(",");
        for (String pair : pairs) {
            int colonIndex = pair.indexOf(":");
            if (colonIndex > 0) {
                String key = pair.substring(0, colonIndex).trim();
                String value = pair.substring(colonIndex + 1).trim();
                result.put(key, value);
            }
        }
        return result;
    }

    private String replaceVariables(String text, Map<String, String> env) {
        if (text == null) return null;
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = env.getOrDefault(key, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public List<ApiTestExecution> getExecutions(Integer caseId) {
        return executionMapper.findRecentByCaseId(caseId, 20);
    }

    @Override
    public ApiTestExecution getLastFailedExecution(Integer caseId) {
        List<ApiTestExecution> executions = executionMapper.findRecentByCaseId(caseId, 20);
        return executions.stream().filter(e -> e.getAssertResult() != null && !e.getAssertResult()).findFirst().orElse(null);
    }

    // ================= AI 生成（根据文档） =================
    @Override
    @Transactional
    public List<ApiModule> generateModulesAndApisFromDoc(Integer projectId, String docText) {
        projectUtils.checkNotArchived(projectId);
        String prompt = buildDocPrompt(docText);
        String aiResponse = chatClient.prompt().user(prompt).call().content();
        List<AIGeneratedModule> generatedModules = parseAIModules(aiResponse);
        if (generatedModules.isEmpty()) {
            throw new RuntimeException("AI未生成有效数据，请检查文档内容");
        }

        List<ApiModule> savedModules = new ArrayList<>();
        for (AIGeneratedModule genModule : generatedModules) {
            ApiModule module = new ApiModule();
            module.setProjectId(projectId);
            module.setModuleName(genModule.getModuleName());
            module.setDescription(genModule.getDescription());
            module.setParentId(0);
            moduleMapper.insert(module);
            savedModules.add(module);

            for (AIGeneratedApi genApi : genModule.getApis()) {
                ApiInfo api = new ApiInfo();
                api.setProjectId(projectId);
                api.setModuleId(module.getId());
                api.setName(genApi.getName());
                api.setMethod(genApi.getMethod());
                api.setUrl(genApi.getUrl());
                api.setHeaders(genApi.getHeaders());
                api.setDescription(genApi.getDescription());
                api.setCreateUserId(getCurrentUserId());
                apiInfoMapper.insert(api);

                for (AIGeneratedTestCase genCase : genApi.getTestCases()) {
                    ApiTestCase testCase = new ApiTestCase();
                    testCase.setApiId(api.getId());
                    testCase.setCaseName(genCase.getCaseName());
                    testCase.setRequestParams(genCase.getRequestParams());
                    testCase.setExpectedStatus(genCase.getExpectedStatus());
                    testCase.setExpectedResponse(genCase.getExpectedResponse());
                    testCase.setAssertType("contains");
                    testCase.setIsGenerated(true);
                    testCase.setCreateUserId(getCurrentUserId());
                    testCaseMapper.insert(testCase);
                }
            }
        }
        return savedModules;
    }

    private String buildDocPrompt(String docText) {
        return "你是一个接口测试专家。请根据以下需求文档，提取出系统应该包含的接口模块、接口定义以及每个接口的测试用例。\n" +
                "输出格式为JSON数组，每个元素表示一个模块，结构如下：\n" +
                "[{\n" +
                "  \"moduleName\": \"模块名称\",\n" +
                "  \"description\": \"模块描述\",\n" +
                "  \"apis\": [\n" +
                "    {\n" +
                "      \"name\": \"接口名称\",\n" +
                "      \"method\": \"GET\",\n" +
                "      \"url\": \"/api/xxx\",\n" +
                "      \"headers\": \"{\\\"Content-Type\\\":\\\"application/json\\\"}\",\n" +
                "      \"description\": \"接口描述\",\n" +
                "      \"testCases\": [\n" +
                "        {\n" +
                "          \"caseName\": \"正常场景\",\n" +
                "          \"requestParams\": \"{\\\"key\\\":\\\"value\\\"}\",\n" +
                "          \"expectedStatus\": 200,\n" +
                "          \"expectedResponse\": \"success\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}]\n" +
                "文档内容：\n" + docText;
    }

    private List<AIGeneratedModule> parseAIModules(String aiResponse) {
        try {
            String json = aiResponse.replaceAll("```json\\s*", "").replaceAll("```", "").trim();
            return objectMapper.readValue(json, new TypeReference<List<AIGeneratedModule>>() {});
        } catch (Exception e) {
            throw new RuntimeException("解析AI响应失败: " + e.getMessage(), e);
        }
    }

    private Integer getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            User user = userMapper.findByUsername(((UserDetails) principal).getUsername());
            return user != null ? user.getId() : null;
        }
        return null;
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
            throw new RuntimeException("不支持的文件类型，请上传 .txt, .md 或 .docx 文件");
        }
    }

    @Override
    public Map<String, String> getEnvVariables(Integer projectId) {
        ApiProjectEnv env = envMapper.findByProjectId(projectId);
        if (env == null || env.getVariables() == null) return new HashMap<>();
        try {
            return objectMapper.readValue(env.getVariables(), new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    @Override
    @Transactional
    public void saveEnvVariables(Integer projectId, Map<String, String> variables) {
        ApiProjectEnv env = envMapper.findByProjectId(projectId);
        String json = null;
        try {
            json = objectMapper.writeValueAsString(variables);
        } catch (Exception e) {
            throw new RuntimeException("序列化失败");
        }
        if (env == null) {
            env = new ApiProjectEnv();
            env.setProjectId(projectId);
            env.setEnvName("default");
            env.setIsDefault(true);
            env.setVariables(json);
            envMapper.insert(env);
        } else {
            env.setVariables(json);
            envMapper.update(env);
        }
    }

    @Override
    @Transactional
    public void batchDeleteExecutions(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        executionMapper.batchDelete(ids);
    }
}