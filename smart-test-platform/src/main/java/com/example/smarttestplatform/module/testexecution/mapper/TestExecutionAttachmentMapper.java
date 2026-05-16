package com.example.smarttestplatform.module.testexecution.mapper;

import com.example.smarttestplatform.module.testexecution.entity.TestExecutionAttachment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TestExecutionAttachmentMapper {

    @Insert("INSERT INTO test_execution_attachment(execution_id, file_name, file_path, file_size, file_type) " +
            "VALUES(#{executionId}, #{fileName}, #{filePath}, #{fileSize}, #{fileType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TestExecutionAttachment attachment);

    @Select("SELECT * FROM test_execution_attachment WHERE execution_id = #{executionId}")
    List<TestExecutionAttachment> findByExecutionId(@Param("executionId") Integer executionId);

    @Delete("DELETE FROM test_execution_attachment WHERE execution_id = #{executionId}")
    int deleteByExecutionId(@Param("executionId") Integer executionId);
}