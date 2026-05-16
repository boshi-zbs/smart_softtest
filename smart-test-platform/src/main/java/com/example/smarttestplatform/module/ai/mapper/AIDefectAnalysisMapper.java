package com.example.smarttestplatform.module.ai.mapper;

import com.example.smarttestplatform.module.ai.entity.AIDefectAnalysis;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AIDefectAnalysisMapper {

    @Select("SELECT * FROM ai_defect_analysis WHERE id = #{id}")
    AIDefectAnalysis findById(Integer id);

    @Select("SELECT * FROM ai_defect_analysis WHERE defect_id = #{defectId} ORDER BY create_time DESC")
    List<AIDefectAnalysis> findByDefectId(Integer defectId);

    @Select("SELECT * FROM ai_defect_analysis WHERE created_by = #{userId} ORDER BY create_time DESC")
    List<AIDefectAnalysis> findByUserId(Integer userId);

    @Insert("INSERT INTO ai_defect_analysis(defect_id, project_id, analysis_type, analysis_result, " +
            "fix_suggestions, affected_files, status, created_by) " +
            "VALUES(#{defectId}, #{projectId}, #{analysisType}, #{analysisResult}, " +
            "#{fixSuggestions}, #{affectedFiles}, #{status}, #{createdBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AIDefectAnalysis analysis);

    @Update("UPDATE ai_defect_analysis SET analysis_result=#{analysisResult}, " +
            "fix_suggestions=#{fixSuggestions}, affected_files=#{affectedFiles}, " +
            "status=#{status} WHERE id=#{id}")
    int update(AIDefectAnalysis analysis);
}