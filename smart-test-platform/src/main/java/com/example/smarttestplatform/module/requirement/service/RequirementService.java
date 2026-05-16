package com.example.smarttestplatform.module.requirement.service;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.requirement.entity.Requirement;

public interface RequirementService {
    Requirement findById(Integer id);
    PageResult<Requirement> pageQuery(PageRequest pageRequest);
    void createRequirement(Requirement requirement, Integer creatorId);
    void updateRequirement(Requirement requirement);
    void deleteRequirement(Integer id);
    void deleteBatch(java.util.List<Integer> ids);
}