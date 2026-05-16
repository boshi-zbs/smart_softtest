package com.example.smarttestplatform.module.git.mapper;

import com.example.smarttestplatform.module.git.entity.ProjectGitConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProjectGitConfigMapper {

    @Select("SELECT * FROM project_git_config WHERE id = #{id}")
    ProjectGitConfig findById(Integer id);

    @Select("SELECT * FROM project_git_config WHERE project_id = #{projectId} AND is_enabled = 1")
    ProjectGitConfig findByProjectId(Integer projectId);

    @Select("SELECT pgc.*, p.project_name as projectName FROM project_git_config pgc " +
            "LEFT JOIN project p ON pgc.project_id = p.id " +
            "WHERE pgc.is_enabled = 1")
    List<ProjectGitConfig> findAllEnabled();

    @Insert("INSERT INTO project_git_config(project_id, repo_url, repo_type, access_token, " +
            "default_branch, local_path, is_enabled) " +
            "VALUES(#{projectId}, #{repoUrl}, #{repoType}, #{accessToken}, " +
            "#{defaultBranch}, #{localPath}, #{isEnabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProjectGitConfig config);

    @Update("UPDATE project_git_config SET repo_url=#{repoUrl}, repo_type=#{repoType}, " +
            "access_token=#{accessToken}, default_branch=#{defaultBranch}, " +
            "local_path=#{localPath}, is_enabled=#{isEnabled}, last_sync_time=#{lastSyncTime} " +
            "WHERE id=#{id}")
    int update(ProjectGitConfig config);

    @Delete("DELETE FROM project_git_config WHERE id = #{id}")
    int deleteById(Integer id);

    @Select("SELECT * FROM project_git_config WHERE project_id = #{projectId}")
    ProjectGitConfig findByProjectIdIgnoreEnabled(Integer projectId);
}