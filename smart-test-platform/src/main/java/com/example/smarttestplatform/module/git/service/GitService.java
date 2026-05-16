package com.example.smarttestplatform.module.git.service;

import com.example.smarttestplatform.module.git.entity.ProjectGitConfig;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class GitService {

    private static final Logger log = LoggerFactory.getLogger(GitService.class);
    public static final String BASE_CLONE_PATH = System.getProperty("user.dir") + "/git-repos/";

    public String cloneOrPullRepository(ProjectGitConfig config) throws Exception {
        String localPath = config.getLocalPath();
        if (!StringUtils.hasText(localPath)) {
            localPath = BASE_CLONE_PATH + config.getProjectId();
            config.setLocalPath(localPath);
        }
        File localRepo = new File(localPath);
        if (localRepo.exists() && new File(localRepo, ".git").exists()) {
            try (Git git = Git.open(localRepo)) {
                StoredConfig repoConfig = git.getRepository().getConfig();
                String currentRemoteUrl = repoConfig.getString("remote", "origin", "url");
                String newRemoteUrl = config.getRepoUrl();
                if (!newRemoteUrl.equals(currentRemoteUrl)) {
                    log.info("检测到仓库地址变更，原地址：{}，新地址：{}，正在更新 remote URL...", currentRemoteUrl, newRemoteUrl);
                    try {
                        git.remoteSetUrl()
                                .setRemoteName("origin")
                                .setRemoteUri(new URIish(newRemoteUrl))
                                .call();
                        log.info("remote URL 更新成功");
                    } catch (URISyntaxException e) {
                        throw new IllegalArgumentException("仓库地址格式错误: " + newRemoteUrl, e);
                    }
                } else {
                    log.debug("remote URL 未变化，无需更新");
                }
            }
            return pullRepository(config);
        } else {
            return cloneRepository(config);
        }
    }

    private String cloneRepository(ProjectGitConfig config) throws Exception {
        File localPath = new File(config.getLocalPath());
        if (!localPath.exists()) {
            localPath.mkdirs();
        }
        try (Git git = Git.cloneRepository()
                .setURI(config.getRepoUrl())
                .setDirectory(localPath)
                .setBranch(config.getDefaultBranch())
                .setCredentialsProvider(buildCredentials(config))
                .call()) {
            log.info("克隆成功：{} -> {}", config.getRepoUrl(), config.getLocalPath());
            return "克隆成功: " + config.getRepoUrl();
        }
    }

    private String pullRepository(ProjectGitConfig config) throws Exception {
        try (Git git = Git.open(new File(config.getLocalPath()))) {
            git.pull()
                    .setCredentialsProvider(buildCredentials(config))
                    .call();
            log.info("拉取成功：{}", config.getRepoUrl());
            return "拉取成功: " + config.getRepoUrl();
        }
    }

    private UsernamePasswordCredentialsProvider buildCredentials(ProjectGitConfig config) {
        if ("private".equals(config.getRepoType()) && StringUtils.hasText(config.getAccessToken())) {
            // Gitee token 认证：用户名固定为 oauth2，密码为 token
            return new UsernamePasswordCredentialsProvider("oauth2", config.getAccessToken());
        }
        return null;
    }

    // 以下方法保持不变（readFileContent, searchFiles, getFileTree, FileNode 等）
    public String readFileContent(ProjectGitConfig config, String filePath) throws IOException {
        String fullPath = config.getLocalPath() + File.separator + filePath;
        File file = new File(fullPath);
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        return new String(Files.readAllBytes(Paths.get(fullPath)));
    }

    public List<String> searchFiles(ProjectGitConfig config, String keyword) throws IOException {
        List<String> results = new ArrayList<>();
        File rootDir = new File(config.getLocalPath());
        if (!rootDir.exists()) {
            return results;
        }
        searchFilesRecursively(rootDir, keyword, results, config.getLocalPath());
        return results;
    }

    private void searchFilesRecursively(File dir, String keyword, List<String> results, String basePath) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                if (!".git".equals(file.getName())) {
                    searchFilesRecursively(file, keyword, results, basePath);
                }
            } else if (isCodeFile(file.getName())) {
                try {
                    String content = new String(Files.readAllBytes(file.toPath()));
                    if (content.contains(keyword)) {
                        String relativePath = file.getAbsolutePath().substring(basePath.length() + 1).replace("\\", "/");
                        results.add(relativePath);
                    }
                } catch (IOException e) {
                    log.warn("读取文件失败: {}", file.getAbsolutePath(), e);
                }
            }
        }
    }

    private boolean isCodeFile(String fileName) {
        String[] extensions = {".java", ".js", ".ts", ".vue", ".jsx", ".tsx", ".py", ".go", ".c", ".cpp", ".h"};
        for (String ext : extensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public List<FileNode> getFileTree(ProjectGitConfig config, String path) throws IOException {
        File currentDir = StringUtils.hasText(path)
                ? new File(config.getLocalPath() + File.separator + path)
                : new File(config.getLocalPath());
        List<FileNode> nodes = new ArrayList<>();
        File[] files = currentDir.listFiles();
        if (files == null) return nodes;
        for (File file : files) {
            if (".git".equals(file.getName())) continue;
            FileNode node = new FileNode();
            node.setName(file.getName());
            String relativePath = file.getAbsolutePath().substring(config.getLocalPath().length() + 1).replace("\\", "/");
            node.setPath(relativePath);
            if (file.isDirectory()) {
                node.setType("directory");
                node.setChildren(getFileTree(config, relativePath));
            } else if (isCodeFile(file.getName())) {
                node.setType("file");
            } else {
                continue;
            }
            nodes.add(node);
        }
        return nodes;
    }

    public static class FileNode {
        private String name;
        private String path;
        private String type;
        private List<FileNode> children;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public List<FileNode> getChildren() { return children; }
        public void setChildren(List<FileNode> children) { this.children = children; }
    }

    /**
     * 根据缺陷标题和描述，智能推荐可能相关的代码文件（基于路径匹配）
     * @return 相关文件的相对路径列表，按相关性降序排序
     */
    public List<String> findRelevantFiles(ProjectGitConfig config, String defectTitle, String defectDescription) throws IOException {
        // 1. 提取关键词
        Set<String> keywords = extractKeywords(defectTitle, defectDescription);
        if (keywords.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 遍历所有代码文件，计算相关性分数
        List<FileScore> scores = new ArrayList<>();
        File root = new File(config.getLocalPath());
        String basePath = root.getAbsolutePath();
        collectFilesForScore(root, basePath, keywords, scores);

        // 3. 按分数降序排序，取前10个
        scores.sort((a, b) -> Integer.compare(b.score, a.score));
        return scores.stream()
                .limit(10)
                .map(fs -> fs.path)
                .collect(Collectors.toList());
    }

    private void collectFilesForScore(File dir, String basePath, Set<String> keywords, List<FileScore> scores) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                if (!".git".equals(file.getName())) {
                    collectFilesForScore(file, basePath, keywords, scores);
                }
            } else if (isCodeFile(file.getName())) {
                String relativePath = file.getAbsolutePath().substring(basePath.length() + 1).replace("\\", "/");
                int score = calculateRelevanceScore(relativePath, keywords);
                if (score > 0) {
                    scores.add(new FileScore(relativePath, score));
                }
            }
        }
    }

    private int calculateRelevanceScore(String filePath, Set<String> keywords) {
        int score = 0;
        String lowerPath = filePath.toLowerCase();
        for (String kw : keywords) {
            // 路径中包含关键词：+2
            if (lowerPath.contains(kw)) {
                score += 2;
            }
            // 文件名（最后一个斜杠后）包含关键词：+3
            String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
            if (fileName.toLowerCase().contains(kw)) {
                score += 3;
            }
        }
        // 额外加权：常见业务层目录优先级更高
        if (lowerPath.contains("/controller/") || lowerPath.contains("/service/") ||
                lowerPath.contains("/mapper/") || lowerPath.contains("/api/")) {
            score += 1;
        }
        // 如果路径中包含测试目录，降低权重（可选）
        if (lowerPath.contains("/test/") || lowerPath.contains("/test/java/")) {
            score -= 1;
        }
        return score;
    }

    private Set<String> extractKeywords(String title, String description) {
        String text = (title + " " + (description != null ? description : "")).toLowerCase();
        String[] words = text.split("[\\s,;:(){}\\[\\]<>!@#$%^&*+=|\\\\/?~`]+");
        Set<String> keywords = new HashSet<>();
        for (String word : words) {
            if (word.length() > 2 && !isCommonStopWord(word)) {
                keywords.add(word);
            }
        }
        return keywords;
    }

    private boolean isCommonStopWord(String word) {
        String[] stopWords = {"the", "and", "for", "with", "this", "that", "from", "have", "are", "was", "were",
                "登录", "报错", "错误", "问题", "失败", "执行", "测试"};
        for (String sw : stopWords) {
            if (sw.equals(word)) return true;
        }
        return false;
    }

    // 内部类
    private static class FileScore {
        String path;
        int score;
        FileScore(String path, int score) { this.path = path; this.score = score; }
    }
}