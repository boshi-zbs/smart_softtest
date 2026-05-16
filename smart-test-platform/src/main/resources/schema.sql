
-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` int NOT NULL AUTO_INCREMENT,
    `username` varchar(50) NOT NULL,
    `password` varchar(100) NOT NULL,
    `real_name` varchar(50) DEFAULT NULL,
    `email` varchar(100) DEFAULT NULL,
    `phone` varchar(20) DEFAULT NULL,
    `status` tinyint DEFAULT '1',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色表
CREATE TABLE IF NOT EXISTS `role` (
    `id` int NOT NULL AUTO_INCREMENT,
    `role_name` varchar(50) NOT NULL,
    `role_code` varchar(50) NOT NULL,
    `description` varchar(200) DEFAULT NULL,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `user_role` (
    `user_id` int NOT NULL,
    `role_id` int NOT NULL,
    PRIMARY KEY (`user_id`,`role_id`),
    CONSTRAINT `fk_ur_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ur_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 后续其他模块的表（按需追加）
-- 项目表
CREATE TABLE IF NOT EXISTS `project` (
    `id` int NOT NULL AUTO_INCREMENT,
    `project_name` varchar(100) NOT NULL,
    `project_key` varchar(50) NOT NULL,
    `description` text,
    `start_date` datetime DEFAULT NULL,
    `end_date` datetime DEFAULT NULL,
    `status` tinyint DEFAULT '1',
    `create_user_id` int DEFAULT NULL,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_key` (`project_key`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 成员表
CREATE TABLE IF NOT EXISTS `project_member` (
    `id` int NOT NULL AUTO_INCREMENT,
    `project_id` int NOT NULL,
    `user_id` int NOT NULL,
    `role_in_project` varchar(50) DEFAULT NULL,
    `join_time` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_user` (`project_id`,`user_id`),
    CONSTRAINT `fk_pm_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_pm_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 需求表
CREATE TABLE IF NOT EXISTS `requirement` (
    `id` int NOT NULL AUTO_INCREMENT,
    `project_id` int NOT NULL,
    `title` varchar(200) NOT NULL,
    `description` text,
    `priority` tinyint DEFAULT '2',
    `status` varchar(20) DEFAULT '待处理',
    `assignee_id` int DEFAULT NULL,
    `creator_id` int DEFAULT NULL,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`),
    KEY `idx_assignee` (`assignee_id`),
    CONSTRAINT `fk_req_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_req_assignee` FOREIGN KEY (`assignee_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_req_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 测试计划表
CREATE TABLE IF NOT EXISTS `test_plan` (
    `id` int NOT NULL AUTO_INCREMENT,
    `project_id` int NOT NULL,
    `plan_name` varchar(100) NOT NULL,
    `description` text,
    `start_date` datetime DEFAULT NULL,
    `end_date` datetime DEFAULT NULL,
    `status` varchar(20) DEFAULT '未开始',
    `creator_id` int DEFAULT NULL,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`),
    CONSTRAINT `fk_tp_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 测试用例表
CREATE TABLE IF NOT EXISTS `test_case` (
    `id` int NOT NULL AUTO_INCREMENT,
    `project_id` int NOT NULL,
    `requirement_id` int DEFAULT NULL,
    `module_id` int DEFAULT NULL,
    `title` varchar(200) NOT NULL,
    `precondition` text,
    `steps` text NOT NULL,
    `expected_result` text NOT NULL,
    `actual_result` text,
    `priority` tinyint DEFAULT '2' COMMENT '1-冒烟，2-高，3-中，4-低',
    `type` varchar(20) DEFAULT '功能测试',
    `status` varchar(20) DEFAULT '有效' COMMENT '有效、无效、废弃',
    `creator_id` int DEFAULT NULL,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`),
    KEY `idx_requirement` (`requirement_id`),
    KEY `fk_tc_creator` (`creator_id`),
    KEY `fk_test_case_module` (`module_id`),
    CONSTRAINT `fk_tc_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_tc_requirement` FOREIGN KEY (`requirement_id`) REFERENCES `requirement` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_tc_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_test_case_module` FOREIGN KEY (`module_id`) REFERENCES `project_module` (`id`) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 测试计划与用例关联表
CREATE TABLE IF NOT EXISTS `plan_case` (
    `id` int NOT NULL AUTO_INCREMENT,
    `plan_id` int NOT NULL,
    `case_id` int NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plan_case` (`plan_id`,`case_id`),
    CONSTRAINT `fk_pc_plan` FOREIGN KEY (`plan_id`) REFERENCES `test_plan` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_pc_case` FOREIGN KEY (`case_id`) REFERENCES `test_case` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 测试任务执行表
CREATE TABLE IF NOT EXISTS `test_execution` (
    `id` int NOT NULL AUTO_INCREMENT,
    `plan_id` int DEFAULT NULL COMMENT '关联测试计划',
    `case_id` int NOT NULL COMMENT '执行的测试用例',
    `executor_id` int DEFAULT NULL COMMENT '执行人',
    `execute_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `result` varchar(20) NOT NULL COMMENT '通过、失败、阻塞、跳过',
    `actual_result` text COMMENT '实际结果描述',
    `duration_ms` int DEFAULT NULL COMMENT '执行耗时（毫秒）',
    `is_automated` tinyint(1) DEFAULT '0' COMMENT '是否自动化执行',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_plan` (`plan_id`),
    KEY `idx_case` (`case_id`),
    KEY `idx_executor` (`executor_id`),
    CONSTRAINT `fk_te_plan` FOREIGN KEY (`plan_id`) REFERENCES `test_plan` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_te_case` FOREIGN KEY (`case_id`) REFERENCES `test_case` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_te_executor` FOREIGN KEY (`executor_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 自动化测试执行记录表
CREATE TABLE IF NOT EXISTS `auto_test_execution` (
    `id` int NOT NULL AUTO_INCREMENT,
    `case_id` int NOT NULL,
    `executor_id` int DEFAULT NULL,
    `start_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `end_time` datetime DEFAULT NULL,
    `status` varchar(20) DEFAULT 'running' COMMENT 'running/success/failed',
    `result` text COMMENT '执行结果日志',
    `ip` varchar(50) DEFAULT NULL COMMENT '执行的IP地址',
    `screenshot_url` varchar(500) DEFAULT NULL COMMENT '执行截图URL',
    PRIMARY KEY (`id`),
    KEY `idx_case` (`case_id`),
    KEY `idx_executor` (`executor_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 缺陷表
CREATE TABLE IF NOT EXISTS `defect` (
    `id` int NOT NULL AUTO_INCREMENT,
    `title` varchar(200) NOT NULL,
    `description` text NOT NULL,
    `severity` varchar(20) DEFAULT '中' COMMENT '致命、严重、一般、轻微',
    `priority` varchar(20) DEFAULT '中' COMMENT '最高、高、中、低',
    `status` varchar(20) DEFAULT '新建' COMMENT '新建、已指派、修复中、已修复、验证中、已关闭、重新打开、驳回',
    `reporter_id` int DEFAULT NULL,
    `assignee_id` int DEFAULT NULL,
    `test_case_id` int DEFAULT NULL,
    `requirement_id` int DEFAULT NULL,
    `project_id` int NOT NULL,
    `found_version` varchar(50) DEFAULT NULL,
    `fixed_version` varchar(50) DEFAULT NULL,
    `auto_execution_id` int DEFAULT NULL COMMENT '关联的自动化执行ID',   -- 新增字段
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_reporter` (`reporter_id`),
    KEY `idx_assignee` (`assignee_id`),
    KEY `idx_test_case` (`test_case_id`),
    KEY `idx_requirement` (`requirement_id`),
    KEY `idx_project` (`project_id`),
    KEY `idx_auto_execution` (`auto_execution_id`),                    -- 可选索引，提高查询效率
    CONSTRAINT `fk_def_reporter` FOREIGN KEY (`reporter_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_def_assignee` FOREIGN KEY (`assignee_id`) REFERENCES `user` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_def_test_case` FOREIGN KEY (`test_case_id`) REFERENCES `test_case` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_def_requirement` FOREIGN KEY (`requirement_id`) REFERENCES `requirement` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_def_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_def_auto_execution` FOREIGN KEY (`auto_execution_id`) REFERENCES `auto_test_execution` (`id`) ON DELETE SET NULL   -- 新增外键
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 缺陷评论表
CREATE TABLE IF NOT EXISTS `defect_comment` (
    `id` int NOT NULL AUTO_INCREMENT,
    `defect_id` int NOT NULL,
    `user_id` int DEFAULT NULL,
    `action` varchar(50) DEFAULT NULL COMMENT '操作类型：评论、状态变更、指派等',
    `old_value` varchar(100) DEFAULT NULL,
    `new_value` varchar(100) DEFAULT NULL,
    `content` text,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_defect` (`defect_id`),
    CONSTRAINT `fk_dc_defect` FOREIGN KEY (`defect_id`) REFERENCES `defect` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` int NOT NULL AUTO_INCREMENT,
    `user_id` int DEFAULT NULL COMMENT '操作用户ID',
    `username` varchar(50) DEFAULT NULL COMMENT '操作用户名',
    `module` varchar(50) NOT NULL COMMENT '操作模块（如用户管理、项目管理等）',
    `operation` varchar(100) NOT NULL COMMENT '操作类型（添加、修改、删除、查询等）',
    `description` varchar(500) DEFAULT NULL COMMENT '操作描述',
    `target_id` int DEFAULT NULL COMMENT '操作对象ID',
    `request_params` text COMMENT '请求参数',
    `result` varchar(20) DEFAULT NULL COMMENT '操作结果（成功/失败）',
    `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
    `user_agent` varchar(500) DEFAULT NULL COMMENT '浏览器信息',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_module` (`module`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

CREATE TABLE IF NOT EXISTS `message` (
    `id` int NOT NULL AUTO_INCREMENT,
    `sender_id` int DEFAULT NULL COMMENT '发送人（系统通知可为NULL）',
    `receiver_id` int NOT NULL COMMENT '接收人',
    `title` varchar(200) NOT NULL,
    `content` text NOT NULL,
    `type` varchar(50) DEFAULT '通知' COMMENT '通知、待办、提醒',
    `related_id` int DEFAULT NULL COMMENT '关联业务ID（如缺陷ID、需求ID等）',
    `related_type` varchar(50) DEFAULT NULL COMMENT '关联业务类型（defect、requirement等）',
    `is_read` tinyint(1) DEFAULT '0' COMMENT '是否已读',
    `read_time` datetime DEFAULT NULL COMMENT '阅读时间',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `status` varchar(20) DEFAULT 'pending' COMMENT 'pending/completed',
    PRIMARY KEY (`id`),
    KEY `idx_receiver` (`receiver_id`),
    KEY `idx_is_read` (`is_read`),
    CONSTRAINT `fk_msg_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 项目与模块关联表
CREATE TABLE IF NOT EXISTS `project_module` (
    `id` int NOT NULL AUTO_INCREMENT,
    `project_id` int NOT NULL,
    `module_name` varchar(100) NOT NULL,
    `description` text,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_project_module` (`project_id`, `module_name`),
    CONSTRAINT `fk_module_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 自动化测试用例表
CREATE TABLE IF NOT EXISTS `auto_test_case` (
    `id` int NOT NULL AUTO_INCREMENT,
    `project_id` int NOT NULL COMMENT '所属项目',
    `case_name` varchar(200) NOT NULL COMMENT '用例名称',
    `description` text COMMENT '描述',
    `url` varchar(500) NOT NULL COMMENT '测试URL（可包含变量如 {ip}）',
    `headless` tinyint DEFAULT '1' COMMENT '运行模式：0-有头模式，1-无头模式',
    `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
    `create_user_id` int DEFAULT NULL,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_project` (`project_id`),
    CONSTRAINT `fk_auto_case_project` FOREIGN KEY (`project_id`) REFERENCES `project` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 自动化测试步骤表
CREATE TABLE IF NOT EXISTS `auto_test_step` (
    `id` int NOT NULL AUTO_INCREMENT,
    `case_id` int NOT NULL COMMENT '所属用例',
    `step_order` int NOT NULL COMMENT '执行顺序',
    `action_type` varchar(50) NOT NULL COMMENT '操作类型：click/input/select/等',
    `locator_type` varchar(30) DEFAULT NULL COMMENT '定位方式：id/name/xpath/css/等',
    `locator_value` varchar(500) DEFAULT NULL COMMENT '定位表达式',
    `input_value` text DEFAULT NULL COMMENT '输入值（用于input等操作）',
    `wait_seconds` int DEFAULT '0' COMMENT '操作前等待秒数',
    `description` varchar(500) DEFAULT NULL COMMENT '步骤描述',
    PRIMARY KEY (`id`),
    KEY `idx_case` (`case_id`),
    CONSTRAINT `fk_auto_step_case` FOREIGN KEY (`case_id`) REFERENCES `auto_test_case` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 项目Git仓库配置表
CREATE TABLE IF NOT EXISTS project_git_config (
                                    id INT PRIMARY KEY AUTO_INCREMENT,
                                    project_id INT NOT NULL COMMENT '项目ID',
                                    repo_url VARCHAR(500) NOT NULL COMMENT 'Git仓库地址',
                                    repo_type VARCHAR(20) DEFAULT 'public' COMMENT '仓库类型：public/private',
                                    access_token VARCHAR(200) COMMENT '私有仓库的Personal Access Token',
                                    default_branch VARCHAR(50) DEFAULT 'main' COMMENT '默认分支',
                                    local_path VARCHAR(500) COMMENT '本地克隆路径',
                                    last_sync_time DATETIME COMMENT '上次同步时间',
                                    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用',
                                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE
);

-- AI分析记录表
CREATE TABLE IF NOT EXISTS ai_defect_analysis (
                                    id INT PRIMARY KEY AUTO_INCREMENT,
                                    defect_id INT NOT NULL COMMENT '缺陷ID',
                                    project_id INT NOT NULL COMMENT '项目ID',
                                    analysis_type VARCHAR(20) DEFAULT 'single' COMMENT '分析类型：single/batch',
                                    analysis_result LONGTEXT COMMENT '分析结果（JSON格式）',
                                    fix_suggestions TEXT COMMENT '修复建议',
                                    affected_files TEXT COMMENT '相关代码文件列表',
                                    status VARCHAR(20) DEFAULT 'completed' COMMENT '状态：pending/completed/failed',
                                    created_by INT COMMENT '创建人ID',
                                    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    FOREIGN KEY (defect_id) REFERENCES defect(id) ON DELETE CASCADE
);

-- AI对话记录表
CREATE TABLE IF NOT EXISTS ai_chat_history (
                                 id INT PRIMARY KEY AUTO_INCREMENT,
                                 session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
                                 defect_id INT COMMENT '关联的缺陷ID',
                                 role VARCHAR(20) NOT NULL COMMENT '角色：user/assistant',
                                 content TEXT NOT NULL COMMENT '对话内容',
                                 create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 INDEX idx_session_id (session_id)
);
--创建附件关联表
CREATE TABLE IF NOT EXISTS test_execution_attachment (
                                                         id INT AUTO_INCREMENT PRIMARY KEY COMMENT '附件ID',
                                                         execution_id INT NOT NULL COMMENT '所属执行记录ID',
                                                         file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径（相对路径或完整URL）',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    file_type VARCHAR(50) COMMENT '文件MIME类型或扩展名',
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    FOREIGN KEY (execution_id) REFERENCES test_execution(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试执行附件表';

-- 新增缺陷附件表
CREATE TABLE IF NOT EXISTS defect_attachment (
                                                 id INT AUTO_INCREMENT PRIMARY KEY COMMENT '附件ID',
                                                 defect_id INT NOT NULL COMMENT '所属缺陷ID',
                                                 file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小（字节）',
    file_type VARCHAR(50) COMMENT '文件MIME类型',
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    FOREIGN KEY (defect_id) REFERENCES defect(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷附件表';