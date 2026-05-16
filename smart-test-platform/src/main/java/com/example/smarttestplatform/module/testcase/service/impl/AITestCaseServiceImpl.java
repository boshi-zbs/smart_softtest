package com.example.smarttestplatform.module.testcase.service.impl;

import com.example.smarttestplatform.module.testcase.service.AITestCaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class AITestCaseServiceImpl implements AITestCaseService {

    @Autowired
    private ChatClient chatClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Map<String, Object>> generateTestCases(MultipartFile file, String content, Integer projectId) {
        // 1. 提取文本内容（优先文件，其次粘贴内容）
        String text = content;
        if (file != null && !file.isEmpty()) {
            try {
                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null) {
                    throw new RuntimeException("无法获取文件名");
                }
                if (originalFilename.endsWith(".txt") || originalFilename.endsWith(".md")) {
                    text = new String(file.getBytes(), StandardCharsets.UTF_8);
                } else if (originalFilename.endsWith(".docx")) {
                    text = extractTextFromDocx(file);
                } else {
                    throw new RuntimeException("不支持的文件格式，仅支持 .txt, .md, .docx");
                }
            } catch (Exception e) {
                throw new RuntimeException("文件读取失败: " + e.getMessage(), e);
            }
        }

        if (text == null || text.isBlank()) {
            throw new RuntimeException("需求内容不能为空");
        }

        // 2. 构建严格的提示词
        String prompt = buildPrompt(text);

        // 3. 调用AI并获取响应
        String aiResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        System.out.println("=== AI原始响应 ===");
        System.out.println(aiResponse);
        System.out.println("=== 响应结束 ===");

        // 4. 解析并返回
        return parseAIResponse(aiResponse);
    }

    /**
     * 从 .docx 文件提取纯文本
     */
    private String extractTextFromDocx(MultipartFile file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String line = paragraph.getText();
                if (line != null && !line.trim().isEmpty()) {
                    sb.append(line).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 构建AI提示词，强调严格基于文档内容生成
     */
    private String buildPrompt(String requirementText) {
        return """
                你是一名资深测试工程师，请**严格、只基于**以下需求文档的内容生成测试用例。
                
                **重要规则**：
                1. 只能使用文档中描述的功能、场景和规则，禁止凭空添加任何文档中没有的内容。
                2. 如果文档内容不完整或模糊，请基于最合理的测试设计原则补充必要的正常场景，但不要超出文档范围。
                3. 输出的JSON数组中，每个用例必须包含以下字段：
                   - title: 用例标题（简洁明确，体现测试点）
                   - precondition: 前置条件（字符串，无则空字符串""）
                   - steps: 测试步骤（字符串，多步骤用 \\n 换行分隔）
                   - expectedResult: 预期结果（字符串）
                   - type: 测试类型，只能从以下选择：功能测试、性能测试、安全测试、兼容性测试
                   - priority: 优先级，数字1-4（1最高，2高，3中，4低）
                4. 输出**纯JSON数组**，不要包含任何解释、注释、Markdown代码块标记（如 ```json）。
                
                需求文档内容如下：
                ----------------------------------------
                %s
                ----------------------------------------
                """.formatted(requirementText);
    }

    /**
     * 解析AI返回的JSON字符串
     */
    private List<Map<String, Object>> parseAIResponse(String aiResponse) {
        // 清理可能的Markdown代码块标记
        String cleaned = aiResponse
                .replaceAll("(?s)^.*?```(?:json)?\\s*", "") // 去除开头的 ```json
                .replaceAll("(?s)\\s*```.*$", "")           // 去除结尾的 ```
                .trim();

        try {
            // 直接尝试解析清理后的内容
            return objectMapper.readValue(cleaned, List.class);
        } catch (Exception e) {
            // 如果失败，尝试用正则提取JSON数组
            String json = cleaned.replaceAll("(?s).*?(\\[.*\\]).*", "$1");
            if (json.equals(cleaned) && !json.startsWith("[")) {
                throw new RuntimeException("AI返回内容无法解析为JSON数组，请重试");
            }
            try {
                return objectMapper.readValue(json, List.class);
            } catch (Exception ex) {
                throw new RuntimeException("AI返回格式解析失败: " + ex.getMessage(), ex);
            }
        }
    }
}