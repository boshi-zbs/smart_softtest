// OperationLogServiceImpl.java
package com.example.smarttestplatform.module.operationlog.service.impl;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.operationlog.entity.OperationLog;
import com.example.smarttestplatform.module.operationlog.mapper.OperationLogMapper;
import com.example.smarttestplatform.module.operationlog.service.OperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Autowired
    private OperationLogMapper operationLogMapper;

    @Override
    public void saveLog(OperationLog log) {
        operationLogMapper.insert(log);
    }

    @Override
    public PageResult<OperationLog> pageQuery(PageRequest pageRequest) {
        Map<String, Object> conditions = pageRequest.getConditions();
        int offset = (pageRequest.getPage() - 1) * pageRequest.getSize();
        List<OperationLog> records = operationLogMapper.pageQuery(conditions, offset, pageRequest.getSize());
        Long total = operationLogMapper.count(conditions);
        return new PageResult<>(records, total, pageRequest.getPage(), pageRequest.getSize());
    }

    @Override
    public List<String> getAllModules() {
        return operationLogMapper.findAllModules();
    }

    @Override
    public List<String> getAllOperations() {
        return operationLogMapper.findAllOperations();
    }

    @Override
    @Transactional
    public void batchDelete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        for (Integer id : ids) {
            operationLogMapper.deleteById(id);
        }
    }

    @Override
    @Transactional
    public void clearAll() {
        operationLogMapper.clearAll();
    }
}