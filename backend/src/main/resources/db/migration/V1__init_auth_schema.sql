-- =====================================================
-- 权限管理模块初始化脚本
-- Phase 1: 角色/权限/用户扩展/智能体扩展
-- =====================================================

-- -----------------------------------------------------
-- 1. 角色表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码: super_admin, admin, user',
    role_name VARCHAR(100) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    role_type VARCHAR(20) DEFAULT 'custom' COMMENT '类型: system(系统内置), custom(自定义)',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- -----------------------------------------------------
-- 2. 权限/菜单表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    perm_code VARCHAR(100) NOT NULL COMMENT '权限编码',
    perm_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    perm_type VARCHAR(20) DEFAULT 'menu' COMMENT '类型: menu(菜单), button(按钮), api(接口)',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID，0为顶级',
    path VARCHAR(200) DEFAULT NULL COMMENT '前端路由路径',
    icon VARCHAR(100) DEFAULT NULL COMMENT '菜单图标',
    component VARCHAR(200) DEFAULT NULL COMMENT '前端组件路径',
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_perm_code (perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- -----------------------------------------------------
-- 3. 角色-权限关联表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联';

-- -----------------------------------------------------
-- 4. 用户-角色关联表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联';

-- -----------------------------------------------------
-- 5. 扩展 users 表
-- -----------------------------------------------------
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS dept_id BIGINT DEFAULT NULL COMMENT '所属部门ID' AFTER role,
    ADD COLUMN IF NOT EXISTS user_code VARCHAR(50) DEFAULT NULL COMMENT 'OA工号' AFTER dept_id,
    ADD COLUMN IF NOT EXISTS phone VARCHAR(20) DEFAULT NULL COMMENT '手机号' AFTER user_code,
    ADD COLUMN IF NOT EXISTS email VARCHAR(100) DEFAULT NULL COMMENT '邮箱' AFTER phone,
    ADD COLUMN IF NOT EXISTS avatar VARCHAR(255) DEFAULT NULL COMMENT '头像' AFTER email,
    ADD COLUMN IF NOT EXISTS status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用' AFTER avatar,
    ADD COLUMN IF NOT EXISTS source VARCHAR(20) DEFAULT 'local' COMMENT '来源: oa_sync(OA同步), local(本地创建), system(系统内置)' AFTER status,
    ADD COLUMN IF NOT EXISTS idafe_login_id VARCHAR(100) DEFAULT NULL COMMENT 'OA/IDAFE登录标识' AFTER source,
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP NULL COMMENT '最后登录时间' AFTER idafe_login_id,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER last_login_at;

-- 给 username 加索引（登录查询用）
ALTER TABLE users ADD INDEX IF NOT EXISTS idx_username (username);
ALTER TABLE users ADD INDEX IF NOT EXISTS idx_idafe_login_id (idafe_login_id);
ALTER TABLE users ADD INDEX IF NOT EXISTS idx_dept_id (dept_id);
ALTER TABLE users ADD INDEX IF NOT EXISTS idx_status (status);

-- -----------------------------------------------------
-- 6. 扩展 departments 表
-- -----------------------------------------------------
ALTER TABLE departments
    ADD COLUMN IF NOT EXISTS parent_id BIGINT DEFAULT 0 COMMENT '父部门ID，0为顶级' AFTER dept_code,
    ADD COLUMN IF NOT EXISTS dept_leader VARCHAR(100) DEFAULT NULL COMMENT '部门负责人' AFTER parent_id,
    ADD COLUMN IF NOT EXISTS sort_order INT DEFAULT 0 AFTER dept_leader,
    ADD COLUMN IF NOT EXISTS status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用' AFTER sort_order,
    ADD COLUMN IF NOT EXISTS source VARCHAR(20) DEFAULT 'local' COMMENT '来源: oa_sync, local' AFTER status,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER source;

ALTER TABLE departments ADD INDEX IF NOT EXISTS idx_parent_id (parent_id);

-- -----------------------------------------------------
-- 7. 扩展 agents 表（可见性控制）
-- -----------------------------------------------------
ALTER TABLE agents
    ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) DEFAULT 'public' COMMENT '可见性: public(全员), role(角色限制), dept(部门限制), private(仅创建者)' AFTER icon_url,
    ADD COLUMN IF NOT EXISTS allowed_roles JSON DEFAULT ('[]') COMMENT '可见角色ID列表' AFTER visibility,
    ADD COLUMN IF NOT EXISTS allowed_depts JSON DEFAULT ('[]') COMMENT '可见部门ID列表' AFTER allowed_roles,
    ADD COLUMN IF NOT EXISTS allowed_users JSON DEFAULT ('[]') COMMENT '可见用户ID白名单' AFTER allowed_depts,
    ADD COLUMN IF NOT EXISTS creator_id BIGINT DEFAULT NULL COMMENT '创建者ID' AFTER allowed_users,
    ADD COLUMN IF NOT EXISTS sort_order INT DEFAULT 0 AFTER creator_id;

ALTER TABLE agents ADD INDEX IF NOT EXISTS idx_visibility (visibility);
ALTER TABLE agents ADD INDEX IF NOT EXISTS idx_creator_id (creator_id);

-- -----------------------------------------------------
-- 8. 初始化系统角色
-- -----------------------------------------------------
INSERT INTO sys_role (id, role_code, role_name, description, role_type, sort_order) VALUES
(1, 'super_admin', '超级管理员', '系统内置超级管理员，拥有一切权限', 'system', 0),
(2, 'admin', '管理员', '系统管理员', 'system', 1),
(3, 'user', '普通用户', '普通用户角色', 'system', 99)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- -----------------------------------------------------
-- 9. 初始化权限菜单
-- -----------------------------------------------------
INSERT INTO sys_permission (id, perm_code, perm_name, perm_type, parent_id, path, sort_order) VALUES
(1, 'dashboard', '工作台', 'menu', 0, '/dashboard', 1),
(2, 'agent_market', '智能体广场', 'menu', 0, '/market', 2),
(3, 'chat', 'AI对话', 'menu', 0, '/chat', 3),
(4, 'workflow', '业务工作流', 'menu', 0, '/workflow', 4),
(5, 'knowledge', '知识库', 'menu', 0, '/knowledge', 5),
(6, 'admin_panel', '系统管理', 'menu', 0, '/admin', 100),
(7, 'admin:agents', '智能体管理', 'menu', 6, '/admin/agents', 1),
(8, 'admin:users', '用户管理', 'menu', 6, '/admin/users', 2),
(9, 'admin:roles', '角色权限', 'menu', 6, '/admin/roles', 3),
(10, 'admin:depts', '部门管理', 'menu', 6, '/admin/depts', 4),
(11, 'admin:stats', '使用统计', 'menu', 6, '/admin/stats', 5),
(12, 'agent:create', '创建智能体', 'button', 7, NULL, 0),
(13, 'agent:edit', '编辑智能体', 'button', 7, NULL, 0),
(14, 'agent:delete', '删除智能体', 'button', 7, NULL, 0),
(15, 'user:create', '创建用户', 'button', 8, NULL, 0),
(16, 'user:edit', '编辑用户', 'button', 8, NULL, 0),
(17, 'user:delete', '删除用户', 'button', 8, NULL, 0),
(18, 'workflow:initiate', '发起流程', 'button', 4, NULL, 0)
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name);

-- -----------------------------------------------------
-- 10. 角色-权限关联
-- -----------------------------------------------------
-- 超级管理员拥有所有权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 管理员拥有基础管理权限
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(2, 1), (2, 2), (2, 3), (2, 4), (2, 5), (2, 6), (2, 7), (2, 8), (2, 10), (2, 11), (2, 12), (2, 13), (2, 14), (2, 15), (2, 16), (2, 17), (2, 18)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- 普通用户只有基础使用权限
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(3, 1), (3, 2), (3, 3), (3, 4), (3, 5), (3, 18)
ON DUPLICATE KEY UPDATE role_id = role_id;

-- -----------------------------------------------------
-- 11. 内置超级管理员账户
-- 密码明文: admin123，使用 BCrypt 加密
-- -----------------------------------------------------
INSERT INTO users (id, username, password, real_name, role, dept_id, user_code, status, source)
VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EO', '系统管理员', 'super_admin', NULL, 'ADMIN001', 1, 'system')
ON DUPLICATE KEY UPDATE
    password = VALUES(password),
    real_name = VALUES(real_name),
    role = VALUES(role),
    status = 1,
    source = 'system';

-- 绑定超级管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1)
ON DUPLICATE KEY UPDATE user_id = user_id;
