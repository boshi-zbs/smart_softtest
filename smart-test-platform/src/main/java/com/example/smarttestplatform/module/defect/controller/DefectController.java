package com.example.smarttestplatform.module.defect.controller;

import com.example.smarttestplatform.common.annotation.Log;
import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.common.service.FileStorageService;
import com.example.smarttestplatform.common.utils.Result;
import com.example.smarttestplatform.module.defect.entity.Defect;
import com.example.smarttestplatform.module.defect.entity.DefectComment;
import com.example.smarttestplatform.module.defect.service.DefectService;
import com.example.smarttestplatform.module.user.entity.User;
import com.example.smarttestplatform.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/defects")
@PreAuthorize("hasAnyRole('ADMIN', 'TESTER', 'DEV')")
public class DefectController {

    @Autowired
    private DefectService defectService;
    @Autowired
    private UserService userService;
    @Autowired
    private FileStorageService fileStorageService;  // ✅ 注入
    @GetMapping
    public Result<PageResult<Defect>> listDefects(@RequestParam Map<String, String> allParams) {
        PageRequest pageRequest = new PageRequest();
        String pageStr = allParams.getOrDefault("page", "1");
        String sizeStr = allParams.getOrDefault("size", "10");
        try {
            pageRequest.setPage(Integer.parseInt(pageStr));
            pageRequest.setSize(Integer.parseInt(sizeStr));
        } catch (NumberFormatException e) {
            pageRequest.setPage(1);
            pageRequest.setSize(10);
        }
        allParams.remove("page");
        allParams.remove("size");
        pageRequest.setConditions(new HashMap<>(allParams));
        PageResult<Defect> pageResult = defectService.pageQuery(pageRequest);
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<Defect> getById(@PathVariable Integer id) {
        Defect defect = defectService.findById(id);
        return defect != null ? Result.success(defect) : Result.error("缺陷不存在");
    }
    @Log(module = "缺陷管理", operation = "创建缺陷", description = "提交新缺陷")
    @PostMapping
    public Result<String> create(@RequestBody Defect defect,
                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) return Result.error("用户不存在");
        defectService.createDefect(defect, user.getId());
        return Result.success("创建成功");
    }
    @Log(module = "缺陷管理", operation = "更新缺陷", description = "更新缺陷信息")
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable Integer id, @RequestBody Defect defect) {
        defect.setId(id);
        defectService.updateDefect(defect);
        return Result.success("更新成功");
    }
    @Log(module = "缺陷管理", operation = "删除缺陷", description = "删除缺陷")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        defectService.deleteDefect(id);
        return Result.success("删除成功");
    }
    @Log(module = "缺陷管理", operation = "批量删除", description = "批量删除缺陷")
    @DeleteMapping("/batch")
    public Result<String> deleteBatch(@RequestBody List<Integer> ids) {
        defectService.deleteBatch(ids);
        return Result.success("批量删除成功");
    }

    @GetMapping("/{id}/comments")
    public Result<List<DefectComment>> getComments(@PathVariable Integer id) {
        return Result.success(defectService.getComments(id));
    }
    @Log(module = "缺陷管理", operation = "添加评论", description = "对缺陷添加评论")
    @PostMapping("/{id}/comments")
    public Result<String> addComment(@PathVariable Integer id,
                                     @RequestBody Map<String, String> body,
                                     @AuthenticationPrincipal UserDetails userDetails) {
        String content = body.get("content");
        if (content == null || content.trim().isEmpty()) {
            return Result.error("评论内容不能为空");
        }
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) return Result.error("用户不存在");
        defectService.addComment(id, user.getId(), content);
        return Result.success("评论成功");
    }
    @Log(module = "缺陷管理", operation = "状态变更", description = "变更缺陷状态")
    @PostMapping("/{id}/status")
    public Result<String> changeStatus(@PathVariable Integer id,
                                       @RequestBody Map<String, String> body,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        String newStatus = body.get("status");
        String comment = body.get("comment");
        if (newStatus == null || newStatus.trim().isEmpty()) {
            return Result.error("状态不能为空");
        }
        User user = userService.findByUsername(userDetails.getUsername());
        if (user == null) return Result.error("用户不存在");
        defectService.changeStatus(id, user.getId(), newStatus, comment);
        return Result.success("状态更新成功");
    }

    // 统计接口（供仪表盘）
    @GetMapping("/statistics/status")
    public Result<Map<String, Integer>> countByStatus(@RequestParam(required = false) Integer projectId) {
        return Result.success(defectService.countByStatus(projectId));
    }

    @GetMapping("/statistics/severity")
    public Result<Map<String, Integer>> countBySeverity(@RequestParam(required = false) Integer projectId) {
        return Result.success(defectService.countBySeverity(projectId));
    }

    @GetMapping("/statistics/priority")
    public Result<Map<String, Integer>> countByPriority(@RequestParam(required = false) Integer projectId) {
        return Result.success(defectService.countByPriority(projectId));
    }
    // ✅ 新增上传附件接口
    @PostMapping("/upload-attachment")
    public Result<Map<String, String>> uploadAttachment(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = fileStorageService.store(file);
            Map<String, String> data = new HashMap<>();
            data.put("url", fileUrl);
            data.put("fileName", file.getOriginalFilename());
            data.put("fileSize", String.valueOf(file.getSize()));
            data.put("fileType", file.getContentType());
            return Result.success(data);
        } catch (IOException e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

}