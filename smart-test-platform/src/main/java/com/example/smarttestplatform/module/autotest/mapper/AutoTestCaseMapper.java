package com.example.smarttestplatform.module.autotest.mapper;

import com.example.smarttestplatform.module.autotest.entity.AutoTestCase;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface AutoTestCaseMapper {

    @Select("SELECT * FROM auto_test_case WHERE id = #{id}")
    AutoTestCase findById(@Param("id") Integer id);

    @Insert("INSERT INTO auto_test_case(project_id, case_name, description, url, headless, status, create_user_id) " +
            "VALUES(#{projectId}, #{caseName}, #{description}, #{url}, #{headless}, #{status}, #{createUserId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AutoTestCase testCase);

    @Update("UPDATE auto_test_case SET project_id=#{projectId}, case_name=#{caseName}, description=#{description}, " +
            "url=#{url}, headless=#{headless}, status=#{status} WHERE id=#{id}")
    int update(AutoTestCase testCase);


    @Delete("DELETE FROM auto_test_case WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    // 分页查询（需在 XML 中实现）
    List<AutoTestCase> pageQuery(@Param("conditions") Map<String, Object> conditions,
                                 @Param("offset") int offset,
                                 @Param("size") int size);

    Long count(@Param("conditions") Map<String, Object> conditions);



    @Select("SELECT COUNT(*) FROM auto_test_case WHERE project_id = #{projectId}")
    int countByProjectId(@Param("projectId") Integer projectId);

}