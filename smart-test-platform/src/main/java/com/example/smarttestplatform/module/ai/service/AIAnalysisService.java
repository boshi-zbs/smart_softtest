package com.example.smarttestplatform.module.ai.service;

import com.example.smarttestplatform.module.defect.entity.Defect;
import com.example.smarttestplatform.module.git.service.GitService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
@Service
public class AIAnalysisService {

    @Autowired
    private ChatClient.Builder chatClientBuilder;  // 注入 Builder

    @Autowired
    private GitService gitService;

    /**
     * 分析缺陷并返回修复建议（非流式）
     */
    public AnalysisResult analyzeDefect(Defect defect, String codeContext, List<String> relatedFiles) {
        String prompt = buildAnalysisPrompt(defect, codeContext, relatedFiles);

        String response = chatClientBuilder.build()  // 通过 Builder 创建 ChatClient
                .prompt()
                .user(prompt)
                .call()
                .content();

        return parseAnalysisResponse(response);
    }

    /**
     * 流式分析缺陷
     */
    public Flux<String> streamAnalyzeDefect(Defect defect, String codeContext, List<String> relatedFiles) {
        String prompt = buildAnalysisPrompt(defect, codeContext, relatedFiles);

        return chatClientBuilder.build()
                .prompt()
                .user(prompt)
                .stream()
                .content();
    }

    private String buildAnalysisPrompt(Defect defect, String codeContext, List<String> relatedFiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位资深的软件工程师和代码审查专家。请分析以下缺陷，并给出具体的修复建议。\n\n");

        sb.append("## 缺陷信息\n");
        sb.append("- 标题：").append(defect.getTitle()).append("\n");
        sb.append("- 描述：").append(defect.getDescription()).append("\n");
        if (defect.getSeverity() != null) {
            sb.append("- 严重程度：").append(defect.getSeverity()).append("\n");
        }
        sb.append("\n");

        if (relatedFiles != null && !relatedFiles.isEmpty()) {
            sb.append("## 相关代码文件\n");
            for (String file : relatedFiles) {
                sb.append("- ").append(file).append("\n");
            }
            sb.append("\n");
        }

        if (codeContext != null && !codeContext.isEmpty()) {
            sb.append("## 相关代码内容\n");
            sb.append("```\n").append(codeContext).append("\n```\n\n");
        }

        sb.append("## 请按以下格式输出分析结果：\n\n");
        sb.append("### 1. 问题定位\n");
        sb.append("（指出缺陷可能发生的代码位置和原因）\n\n");
        sb.append("### 2. 修复方案\n");
        sb.append("（提供具体的代码修改建议）\n\n");
        sb.append("### 3. 注意事项\n");
        sb.append("（修复时需要注意的其他问题）\n");

        return sb.toString();
    }

    private AnalysisResult parseAnalysisResponse(String response) {
        AnalysisResult result = new AnalysisResult();
        result.setRawResponse(response);

        // 尝试匹配 ### 1. 问题定位 ... ### 2. 修复方案 ...
        String[] sections = response.split("###\\s*\\d+\\.");
        if (sections.length >= 2) {
            // sections[0] 是标题之前的杂项，忽略
            for (int i = 1; i < sections.length; i++) {
                String section = sections[i];
                if (section.contains("问题定位")) {
                    result.setProblemLocation(extractContent(section));
                } else if (section.contains("修复方案")) {
                    result.setFixSuggestion(extractContent(section));
                } else if (section.contains("注意事项")) {
                    result.setPrecautions(extractContent(section));
                }
            }
        }

        // 如果仍然为空，则尝试基于常见关键词提取
        if (result.getProblemLocation() == null) {
            result.setProblemLocation(extractByKeyword(response, "问题定位|问题分析|原因分析"));
        }
        if (result.getFixSuggestion() == null) {
            result.setFixSuggestion(extractByKeyword(response, "修复方案|解决方法|修改建议"));
        }
        if (result.getPrecautions() == null) {
            result.setPrecautions(extractByKeyword(response, "注意事项|风险提示"));
        }

        // 最终保底：将所有内容放入修复建议
        if (result.getFixSuggestion() == null && result.getProblemLocation() == null) {
            result.setFixSuggestion(response);
        }
        return result;
    }

    private String extractContent(String section) {
        // 去掉开头可能残留的标题文字
        int start = section.indexOf("\n");
        if (start != -1) {
            section = section.substring(start).trim();
        }
        // 截取到下一个 ### 或结尾
        int end = section.indexOf("###");
        if (end != -1) {
            section = section.substring(0, end).trim();
        }
        return section;
    }

    private String extractByKeyword(String text, String keywords) {
        Pattern pattern = Pattern.compile("(?i)(?<=(" + keywords + "))[\\s\\S]*?(?=\\n\\n|$)", Pattern.MULTILINE);
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return m.group().trim();
        }
        return null;
    }

    /**
     * 通用对话（流式）
     */
    public Flux<String> chat(String sessionId, String userMessage, Integer defectId,
                             String defectTitle, String codeContext) {
        String systemPrompt = buildChatSystemPrompt(defectTitle, codeContext);

        return chatClientBuilder.build()
                .prompt()
                .system(systemPrompt)
                .user(userMessage)
                .stream()
                .content();
    }

    private String buildChatSystemPrompt(String defectTitle, String codeContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位技术专家，正在帮助开发人员修复缺陷。\n\n");

        if (defectTitle != null) {
            sb.append("相关缺陷：").append(defectTitle).append("\n\n");
        }

        if (codeContext != null && !codeContext.isEmpty()) {
            sb.append("代码上下文：\n```\n").append(codeContext).append("\n```\n\n");
        }

        sb.append("请用中文回答，提供专业、清晰的建议。");
        return sb.toString();
    }

    @lombok.Data
    public static class AnalysisResult {
        private String problemLocation;
        private String fixSuggestion;
        private String precautions;
        private String rawResponse;
    }
}