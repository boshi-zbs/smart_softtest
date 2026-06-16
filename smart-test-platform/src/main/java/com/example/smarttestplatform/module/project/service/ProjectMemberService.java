package com.example.smarttestplatform.module.project.service;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.project.entity.ProjectMember;

import java.util.List;

public interface ProjectMemberService {
    List<ProjectMember> findByProjectId(Integer projectId);
    PageResult<ProjectMember> pageQuery(PageRequest pageRequest);
    void addMember(ProjectMember projectMember, Integer operatorId);
    void updateMember(ProjectMember projectMember);
    void removeMember(Integer projectId, Integer userId);
    void removeBatch(List<ProjectMember> members);
}