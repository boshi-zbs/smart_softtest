package com.example.smarttestplatform.module.git.controller;

import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.git.entity.ProjectGitConfig;
import com.example.smarttestplatform.module.git.mapper.ProjectGitConfigMapper;
import com.example.smarttestplatform.module.git.service.GitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/git-config")
@PreAuthorize("hasRole('ADMIN')")
public class GitConfigController {

    @Autowired
    private ProjectGitConfigMapper gitConfigMapper;

    @Autowired
    private GitService gitService;

    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public Result<ProjectGitConfig> getByProject(@PathVariable Integer projectId) {
        ProjectGitConfig config = gitConfigMapper.findByProjectId(projectId);
        return Result.success(config);
    }

    @GetMapping
    public Result<List<ProjectGitConfig>> listAll() {
        return Result.success(gitConfigMapper.findAllEnabled());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> save(@RequestBody ProjectGitConfig config) {
        // 确保启用状态为 1
        config.setIsEnabled(1);
        ProjectGitConfig existing = gitConfigMapper.findByProjectId(config.getProjectId());
        if (existing != null) {
            config.setId(existing.getId());
            gitConfigMapper.update(config);
        } else {
            gitConfigMapper.insert(config);
        }
        return Result.success("保存成功");
    }

    @PostMapping("/sync/{projectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> syncRepository(@PathVariable Integer projectId) throws Exception {
        ProjectGitConfig config = gitConfigMapper.findByProjectId(projectId);
        if (config == null) {
            return Result.error("请先配置Git仓库");
        }
        String result = gitService.cloneOrPullRepository(config);
        config.setLastSyncTime(new Date());
        gitConfigMapper.update(config);
        return Result.success(result);
    }

    @GetMapping("/file-tree/{projectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEV')")
    public Result<List<GitService.FileNode>> getFileTree(@PathVariable Integer projectId) throws Exception {
        ProjectGitConfig config = gitConfigMapper.findByProjectId(projectId);
        if (config == null) {
            return Result.error("请先配置Git仓库");
        }
        // 确保仓库已克隆
        gitService.cloneOrPullRepository(config);
        List<GitService.FileNode> tree = gitService.getFileTree(config, null);
        return Result.success(tree);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<String> delete(@PathVariable Integer id) {
        gitConfigMapper.deleteById(id);
        return Result.success("删除成功");
    }
}