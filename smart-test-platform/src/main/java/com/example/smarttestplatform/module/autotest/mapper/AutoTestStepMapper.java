package com.example.smarttestplatform.module.autotest.mapper;

import com.example.smarttestplatform.module.autotest.entity.AutoTestStep;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AutoTestStepMapper {

    @Select("SELECT * FROM auto_test_step WHERE case_id = #{caseId} ORDER BY step_order")
    List<AutoTestStep> findByCaseId(@Param("caseId") Integer caseId);

    @Insert("INSERT INTO auto_test_step(case_id, step_order, action_type, locator_type, locator_value, input_value, wait_seconds, description) " +
            "VALUES(#{caseId}, #{stepOrder}, #{actionType}, #{locatorType}, #{locatorValue}, #{inputValue}, #{waitSeconds}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AutoTestStep step);

    @Update("UPDATE auto_test_step SET action_type=#{actionType}, locator_type=#{locatorType}, locator_value=#{locatorValue}, " +
            "input_value=#{inputValue}, wait_seconds=#{waitSeconds}, description=#{description} WHERE id=#{id}")
    int update(AutoTestStep step);

    @Delete("DELETE FROM auto_test_step WHERE id = #{id}")
    int deleteById(@Param("id") Integer id);

    @Delete("DELETE FROM auto_test_step WHERE case_id = #{caseId}")
    int deleteByCaseId(@Param("caseId") Integer caseId);
}