package com.example.smarttestplatform.module.defect.service;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.defect.entity.Defect;
import com.example.smarttestplatform.module.defect.entity.DefectComment;

import java.util.List;
import java.util.Map;

public interface DefectService {
    Defect findById(Integer id);
    PageResult<Defect> pageQuery(PageRequest pageRequest);
    void createDefect(Defect defect, Integer reporterId);
    void updateDefect(Defect defect);
    void deleteDefect(Integer id);
    void deleteBatch(List<Integer> ids);

    // 评论相关
    void addComment(Integer defectId, Integer userId, String content);
    List<DefectComment> getComments(Integer defectId);
    // 状态变更（会记录评论）
    void changeStatus(Integer defectId, Integer userId, String newStatus, String comment);
    // 统计（用于仪表盘）
    Map<String, Integer> countByStatus(Integer projectId);
    Map<String, Integer> countBySeverity(Integer projectId);
    Map<String, Integer> countByPriority(Integer projectId);
}