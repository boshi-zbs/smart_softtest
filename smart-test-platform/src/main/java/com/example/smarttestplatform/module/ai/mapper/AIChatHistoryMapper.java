package com.example.smarttestplatform.module.ai.mapper;

import com.example.smarttestplatform.module.ai.entity.AIChatHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AIChatHistoryMapper {

    @Select("SELECT * FROM ai_chat_history WHERE session_id = #{sessionId} ORDER BY create_time")
    List<AIChatHistory> findBySessionId(String sessionId);

    @Insert("INSERT INTO ai_chat_history(session_id, defect_id, role, content) " +
            "VALUES(#{sessionId}, #{defectId}, #{role}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AIChatHistory history);
}