package com.example.smarttestplatform.module.defect.mapper;

import com.example.smarttestplatform.module.defect.entity.DefectComment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DefectCommentMapper {

    @Insert("INSERT INTO defect_comment(defect_id, user_id, action, old_value, new_value, content) " +
            "VALUES(#{defectId}, #{userId}, #{action}, #{oldValue}, #{newValue}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DefectComment comment);

    @Select("SELECT * FROM defect_comment WHERE defect_id = #{defectId} ORDER BY create_time DESC")
    List<DefectComment> findByDefectId(@Param("defectId") Integer defectId);
}