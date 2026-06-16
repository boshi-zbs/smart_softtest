package com.example.smarttestplatform.module.apitester.mapper;

import com.example.smarttestplatform.module.apitester.entity.ApiInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ApiInfoMapper {
    @Select("SELECT * FROM api_info WHERE id = #{id}")
    ApiInfo findById(Integer id);

    List<ApiInfo> listByModule(@Param("moduleId") Integer moduleId, @Param("projectId") Integer projectId);

    @Insert("INSERT INTO api_info(project_id, module_id, name, method, url, headers, description, create_user_id) VALUES(#{projectId}, #{moduleId}, #{name}, #{method}, #{url}, #{headers}, #{description}, #{createUserId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ApiInfo apiInfo);

    @Update("UPDATE api_info SET name=#{name}, method=#{method}, url=#{url}, headers=#{headers}, description=#{description} WHERE id=#{id}")
    int update(ApiInfo apiInfo);

    @Delete("DELETE FROM api_info WHERE id = #{id}")
    int deleteById(Integer id);
}