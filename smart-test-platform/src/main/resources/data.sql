

-- 插入角色数据（如果不存在）
INSERT IGNORE INTO `role` (`role_name`, `role_code`, `description`) VALUES
('管理员', 'ROLE_ADMIN', '系统管理员'),
('测试人员', 'ROLE_TESTER', '测试人员'),
('开发人员', 'ROLE_DEV', '开发人员');

-- 插入管理员用户（密码 123456 已 BCrypt 加密）
INSERT IGNORE INTO `user` (`username`, `password`, `real_name`, `email`, `phone`, `status`)
VALUES ('admin', '$2a$10$cQADWkgvKU9m3An7ZFj6kuJfmix3GQAWqSqiEN4v2tP.Izkv9f1tu', '系统管理员', 'admin@test.com', '13800138000', 1);

-- 关联管理员用户与管理员角色（动态获取角色ID）
INSERT IGNORE INTO `user_role` (`user_id`, `role_id`)
SELECT u.id, r.id FROM `user` u, `role` r
WHERE u.username = 'admin' AND r.role_code = 'ROLE_ADMIN';