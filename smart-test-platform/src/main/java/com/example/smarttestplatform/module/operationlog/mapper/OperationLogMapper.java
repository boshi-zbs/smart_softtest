// OperationLogMapper.java
package com.example.smarttestplatform.module.operationlog.mapper;

import com.example.smarttestplatform.module.operationlog.entity.OperationLog;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface OperationLogMapper {

    @Insert("INSERT INTO operation_log(user_id, username, module, operation, description, target_id, request_params, result, ip_address, user_agent) " +
            "VALUES(#{userId}, #{username}, #{module}, #{operation}, #{description}, #{targetId}, #{requestParams}, #{result}, #{ipAddress}, #{userAgent})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OperationLog log);

    List<OperationLog> pageQuery(@Param("conditions") Map<String, Object> conditions,
                                 @Param("offset") int offset,
                                 @Param("size") int size);

    Long count(@Param("conditions") Map<String, Object> conditions);

    // 获取所有唯一的模块名
    @Select("SELECT DISTINCT module FROM operation_log ORDER BY module")
    List<String> findAllModules();

    // 获取所有唯一的操作类型
    @Select("SELECT DISTINCT operation FROM operation_log ORDER BY operation")
    List<String> findAllOperations();

    @Delete("DELETE FROM operation_log WHERE id = #{id}")
    int deleteById(Integer id);

    @Delete("DELETE FROM operation_log")
    int clearAll();
}