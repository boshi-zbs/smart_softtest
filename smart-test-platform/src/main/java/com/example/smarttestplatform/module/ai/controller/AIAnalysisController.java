package com.example.smarttestplatform.module.ai.controller;

import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.ai.entity.AIDefectAnalysis;
import com.example.smarttestplatform.module.ai.entity.AIChatHistory;
import com.example.smarttestplatform.module.ai.mapper.AIDefectAnalysisMapper;
import com.example.smarttestplatform.module.ai.mapper.AIChatHistoryMapper;
import com.example.smarttestplatform.module.ai.service.AIAnalysisService;
import com.example.smarttestplatform.module.defect.entity.Defect;
import com.example.smarttestplatform.module.defect.mapper.DefectMapper;
import com.example.smarttestplatform.module.git.entity.ProjectGitConfig;
import com.example.smarttestplatform.module.git.mapper.ProjectGitConfigMapper;
import com.example.smarttestplatform.module.git.service.GitService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai-analysis")
public class AIAnalysisController {

    private static final Logger log = LoggerFactory.getLogger(AIAnalysisController.class);

    @Autowired
    private AIAnalysisService aiAnalysisService;

    @Autowired
    private DefectMapper defectMapper;

    @Autowired
    private ProjectGitConfigMapper gitConfigMapper;

    @Autowired
    private GitService gitService;

    @Autowired
    private AIDefectAnalysisMapper analysisMapper;

    @Autowired
    private AIChatHistoryMapper chatHistoryMapper;

    @Autowired
    private UserService userService;

    /**
     * 获取项目关联的Git仓库文件树
     */
    @GetMapping("/code-tree/{projectId}")
    public Result<List<GitService.FileNode>> getCodeTree(@PathVariable Integer projectId) throws Exception {
        ProjectGitConfig config = gitConfigMapper.findByProjectId(projectId);
        if (config == null) {
            return Result.error("该项目未配置Git仓库");
        }
        gitService.cloneOrPullRepository(config);
        List<GitService.FileNode> tree = gitService.getFileTree(config, null);
        return Result.success(tree);
    }

    /**
     * 获取代码文件内容
     */
    @GetMapping("/code-content/{projectId}")
    public Result<String> getCodeContent(@PathVariable Integer projectId,
                                         @RequestParam String filePath) throws Exception {
        ProjectGitConfig config = gitConfigMapper.findByProjectId(projectId);
        if (config == null) {
            return Result.error("该项目未配置Git仓库");
        }
        String content = gitService.readFileContent(config, filePath);
        return Result.success(content);
    }

    /**
     * 根据关键词搜索相关代码文件
     */
    @GetMapping("/search-code/{projectId}")
    public Result<List<String>> searchCodeFiles(@PathVariable Integer projectId,
                                                @RequestParam String keyword) throws Exception {
        ProjectGitConfig config = gitConfigMapper.findByProjectId(projectId);
        if (config == null) {
            return Result.error("该项目未配置Git仓库");
        }
        gitService.cloneOrPullRepository(config);
        List<String> files = gitService.searchFiles(config, keyword);
        return Result.success(files);
    }

    /**
     * 分析单个缺陷（非流式）
     */
    /**
     * 分析单个缺陷（非流式）
     * 优先使用前端勾选的文件；若未勾选则自动推荐相关代码文件（基于路径匹配）
     */
    @PostMapping("/analyze-single")
    public Result<Map<String, Object>> analyzeSingleDefect(@RequestBody AnalyzeRequest request,
                                                           @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        // 1. 获取当前用户
        User user = userService.findByUsername(userDetails.getUsername());

        // 2. 查询缺陷
        Defect defect = defectMapper.findById(request.getDefectId());
        if (defect == null) {
            return Result.error("缺陷不存在");
        }

        // 3. 获取项目的 Git 配置
        ProjectGitConfig gitConfig = gitConfigMapper.findByProjectId(defect.getProjectId());
        if (gitConfig == null) {
            return Result.error("该项目未配置Git仓库");
        }

        // 4. 确保本地仓库已克隆/拉取最新代码
        gitService.cloneOrPullRepository(gitConfig);

        // 5. 确定要分析的文件列表和代码上下文
        List<String> filePaths = request.getFilePaths();
        StringBuilder codeContext = new StringBuilder();

        if (filePaths != null && !filePaths.isEmpty()) {
            // 用户手动勾选了文件
            for (String filePath : filePaths) {
                String content = gitService.readFileContent(gitConfig, filePath);
                if (content != null && !content.isEmpty()) {
                    codeContext.append("文件: ").append(filePath).append("\n");
                    codeContext.append("```\n").append(content).append("\n```\n\n");
                }
            }
        } else {
            // 用户未勾选，自动推荐相关文件（基于路径匹配，不读取内容）
            List<String> recommended = gitService.findRelevantFiles(gitConfig, defect.getTitle(), defect.getDescription());
            if (recommended.isEmpty()) {
                return Result.error("无法自动定位相关代码文件，请手动勾选相关文件后重试");
            }
            // 最多取前5个文件的内容
            filePaths = recommended.stream().limit(5).collect(Collectors.toList());
            for (String filePath : filePaths) {
                String content = gitService.readFileContent(gitConfig, filePath);
                if (content != null && !content.isEmpty()) {
                    codeContext.append("文件: ").append(filePath).append("\n");
                    codeContext.append("```\n").append(content).append("\n```\n\n");
                }
            }
        }

        // 6. 调用 AI 分析服务
        AIAnalysisService.AnalysisResult result = aiAnalysisService.analyzeDefect(defect, codeContext.toString(), filePaths);

        // 7. 保存分析记录到数据库
        AIDefectAnalysis analysis = new AIDefectAnalysis();
        analysis.setDefectId(defect.getId());
        analysis.setProjectId(defect.getProjectId());
        analysis.setAnalysisType("single");
        analysis.setAnalysisResult(result.getRawResponse());
        analysis.setFixSuggestions(result.getFixSuggestion());
        analysis.setAffectedFiles(filePaths != null ? String.join(",", filePaths) : null);
        analysis.setStatus("completed");
        analysis.setCreatedBy(user.getId());
        analysisMapper.insert(analysis);

        // 8. 构建返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("analysisId", analysis.getId());
        response.put("problemLocation", result.getProblemLocation());
        response.put("fixSuggestion", result.getFixSuggestion());
        response.put("precautions", result.getPrecautions());
        response.put("fullResponse", result.getRawResponse());
        response.put("affectedFiles", filePaths);

        return Result.success(response);
    }

    /**
     * 批量分析缺陷
     */
    @PostMapping("/analyze-batch")
    public Result<List<Map<String, Object>>> analyzeBatchDefects(@RequestBody BatchAnalyzeRequest request,
                                                                 @AuthenticationPrincipal UserDetails userDetails) throws Exception {
        User user = userService.findByUsername(userDetails.getUsername());
        List<Map<String, Object>> results = new ArrayList<>();

        for (Integer defectId : request.getDefectIds()) {
            Defect defect = defectMapper.findById(defectId);
            if (defect == null) continue;

            ProjectGitConfig gitConfig = gitConfigMapper.findByProjectId(defect.getProjectId());
            if (gitConfig == null) continue;

            gitService.cloneOrPullRepository(gitConfig);

            String keyword = extractKeyword(defect.getTitle(), defect.getDescription());
            List<String> filePaths = null;
            StringBuilder codeContext = new StringBuilder();

            if (keyword != null) {
                filePaths = gitService.searchFiles(gitConfig, keyword);
                if (filePaths != null && !filePaths.isEmpty()) {
                    filePaths = filePaths.stream().limit(3).collect(Collectors.toList());
                    for (String filePath : filePaths) {
                        String content = gitService.readFileContent(gitConfig, filePath);
                        if (content != null) {
                            codeContext.append("文件: ").append(filePath).append("\n```\n").append(content).append("\n```\n\n");
                        }
                    }
                }
            }

            AIAnalysisService.AnalysisResult result = aiAnalysisService.analyzeDefect(defect, codeContext.toString(), filePaths);

            AIDefectAnalysis analysis = new AIDefectAnalysis();
            analysis.setDefectId(defectId);
            analysis.setProjectId(defect.getProjectId());
            analysis.setAnalysisType("batch");
            analysis.setAnalysisResult(result.getRawResponse());
            analysis.setFixSuggestions(result.getFixSuggestion());
            analysis.setStatus("completed");
            analysis.setCreatedBy(user.getId());
            analysisMapper.insert(analysis);

            Map<String, Object> item = new HashMap<>();
            item.put("defectId", defectId);
            item.put("defectTitle", defect.getTitle());
            item.put("analysisId", analysis.getId());
            item.put("fixSuggestion", result.getFixSuggestion() != null ? result.getFixSuggestion().substring(0, Math.min(200, result.getFixSuggestion().length())) : "");
            results.add(item);
        }

        return Result.success(results);
    }

    /**
     * 流式分析缺陷（用于前端实时显示）
     */
    @PostMapping(value = "/analyze-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamAnalyzeDefect(@RequestBody AnalyzeRequest request) throws Exception {
        Defect defect = defectMapper.findById(request.getDefectId());
        if (defect == null) {
            return Flux.just("错误：缺陷不存在");
        }

        ProjectGitConfig gitConfig = gitConfigMapper.findByProjectId(defect.getProjectId());
        if (gitConfig == null) {
            return Flux.just("错误：该项目未配置Git仓库");
        }

        gitService.cloneOrPullRepository(gitConfig);

        StringBuilder codeContext = new StringBuilder();
        List<String> filePaths = request.getFilePaths();
        if (filePaths != null && !filePaths.isEmpty()) {
            for (String filePath : filePaths) {
                String content = gitService.readFileContent(gitConfig, filePath);
                if (content != null) {
                    codeContext.append("文件: ").append(filePath).append("\n");
                    codeContext.append("```\n").append(content).append("\n```\n\n");
                }
            }
        } else {
            String keyword = extractKeyword(defect.getTitle(), defect.getDescription());
            if (keyword != null) {
                List<String> relatedFiles = gitService.searchFiles(gitConfig, keyword);
                if (!relatedFiles.isEmpty()) {
                    filePaths = relatedFiles.stream().limit(5).collect(Collectors.toList());
                    for (String filePath : filePaths) {
                        String content = gitService.readFileContent(gitConfig, filePath);
                        if (content != null) {
                            codeContext.append("文件: ").append(filePath).append("\n");
                            codeContext.append("```\n").append(content).append("\n```\n\n");
                        }
                    }
                }
            }
        }

        return aiAnalysisService.streamAnalyzeDefect(defect, codeContext.toString(), filePaths);
    }

    /**
     * AI对话（流式）
     */
    @PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null) sessionId = UUID.randomUUID().toString();

        // 保存用户消息（可选）
        AIChatHistory userHistory = new AIChatHistory();
        userHistory.setSessionId(sessionId);
        userHistory.setDefectId(request.getDefectId());
        userHistory.setRole("user");
        userHistory.setContent(request.getMessage());
        chatHistoryMapper.insert(userHistory);

        String defectTitle = null;
        String codeContext = null;
        ProjectGitConfig gitConfig = null;

        if (request.getDefectId() != null) {
            Defect defect = defectMapper.findById(request.getDefectId());
            if (defect != null) {
                defectTitle = defect.getTitle();
                gitConfig = gitConfigMapper.findByProjectId(defect.getProjectId());
            }
        }

        // 如果有文件路径，读取代码内容作为上下文
        if (gitConfig != null && request.getFilePaths() != null && !request.getFilePaths().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String path : request.getFilePaths()) {
                try {
                    String content = gitService.readFileContent(gitConfig, path);
                    if (content != null && !content.isEmpty()) {
                        sb.append("文件：").append(path).append("\n```\n").append(content).append("\n```\n\n");
                    }
                } catch (Exception e) {
                    log.warn("读取文件失败: {}", path, e);
                }
            }
            codeContext = sb.toString();
        }

        return aiAnalysisService.chat(sessionId, request.getMessage(), request.getDefectId(), defectTitle, codeContext)
                .doOnComplete(() -> {
                    // 可以保存 AI 回复到数据库（需要累积完整内容，这里略）
                });
    }

    /**
     * 获取分析历史记录
     */
    @GetMapping("/history")
    public Result<List<AIDefectAnalysis>> getAnalysisHistory(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        List<AIDefectAnalysis> history = analysisMapper.findByUserId(user.getId());
        return Result.success(history);
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/chat-history/{sessionId}")
    public Result<List<AIChatHistory>> getChatHistory(@PathVariable String sessionId) {
        List<AIChatHistory> history = chatHistoryMapper.findBySessionId(sessionId);
        return Result.success(history);
    }

    private String extractKeyword(String title, String description) {
        // 从标题和描述中提取关键词用于搜索代码
        String text = (title + " " + (description != null ? description : "")).toLowerCase();
        // 提取类名、方法名等（简单实现）
        String[] words = text.split("[\\\\s,;:(){}\\[\\]<>!@#$%^&*+=|\\\\/?~`]+");
        for (String word : words) {
            if (word.length() > 3 && !isStopWord(word)) {
                return word;
            }
        }
        return null;
    }

    private boolean isStopWord(String word) {
        String[] stopWords = {"the", "and", "for", "with", "this", "that", "from", "have", "are", "was", "were"};
        for (String sw : stopWords) {
            if (sw.equals(word)) return true;
        }
        return false;
    }

    @lombok.Data
    public static class AnalyzeRequest {
        private Integer defectId;
        private List<String> filePaths;
    }

    @lombok.Data
    public static class BatchAnalyzeRequest {
        private List<Integer> defectIds;
        private List<String> filePaths;
    }

    @lombok.Data
    public static class ChatRequest {
        private String sessionId;
        private Integer defectId;
        private String message;
        private List<String> filePaths;  // 新增
    }
}