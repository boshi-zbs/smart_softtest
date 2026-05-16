package com.example.smarttestplatform.module.defect.mapper;

import com.example.smarttestplatform.module.defect.entity.DefectAttachment;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface DefectAttachmentMapper {

    @Insert("INSERT INTO defect_attachment(defect_id, file_name, file_path, file_size, file_type) " +
            "VALUES(#{defectId}, #{fileName}, #{filePath}, #{fileSize}, #{fileType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DefectAttachment attachment);

    @Select("SELECT * FROM defect_attachment WHERE defect_id = #{defectId}")
    List<DefectAttachment> findByDefectId(@Param("defectId") Integer defectId);

    @Delete("DELETE FROM defect_attachment WHERE defect_id = #{defectId}")
    int deleteByDefectId(@Param("defectId") Integer defectId);
}