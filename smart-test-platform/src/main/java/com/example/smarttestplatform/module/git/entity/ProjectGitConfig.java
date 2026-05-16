package com.example.smarttestplatform.module.git.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ProjectGitConfig {
    private Integer id;
    private Integer projectId;
    private String repoUrl;
    private String repoType;
    private String accessToken;
    private String defaultBranch;
    private String localPath;
    private Date lastSyncTime;
    private Integer isEnabled;
    private Date createTime;
    private Date updateTime;

    // 非数据库字段
    private String projectName;
}