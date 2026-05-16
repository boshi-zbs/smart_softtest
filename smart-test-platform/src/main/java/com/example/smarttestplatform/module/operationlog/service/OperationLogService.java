// OperationLogService.java
package com.example.smarttestplatform.module.operationlog.service;

import com.example.smarttestplatform.common.dto.PageRequest;
import com.example.smarttestplatform.common.dto.PageResult;
import com.example.smarttestplatform.module.operationlog.entity.OperationLog;

import java.util.List;

public interface OperationLogService {
    void saveLog(OperationLog log);
    PageResult<OperationLog> pageQuery(PageRequest pageRequest);
    List<String> getAllModules();
    List<String> getAllOperations();

    void batchDelete(List<Integer> ids);
    void clearAll();
}