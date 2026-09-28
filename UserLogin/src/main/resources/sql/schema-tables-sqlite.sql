-- ==============================================
-- SQLite初始化脚本（纯SQLite兼容语法）
-- ==============================================

-- 1. 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
                                        user_id INTEGER PRIMARY KEY AUTOINCREMENT, -- 用户ID（自增）
                                        name TEXT DEFAULT NULL, -- 用户姓名
    -- 核心修改：添加 DEFAULT '男'，和MySQL enum第一个值对齐
                                        sex TEXT NOT NULL DEFAULT '男' CHECK(sex IN ('男','女','未知')), -- 用户性别
                                        account TEXT NOT NULL, -- 登录账号（唯一）
                                        password TEXT NOT NULL, -- 登录密码（Bcrypt加密）
                                        status INTEGER NOT NULL, -- 用户状态：1=启用，0=禁用
                                        disable INTEGER NOT NULL, -- 是否禁用
                                        ctime DATETIME DEFAULT CURRENT_TIMESTAMP, -- 创建时间
                                        UNIQUE (account)
);

-- 插入初始用户（SQLite兼容写法）
INSERT OR IGNORE INTO sys_user (user_id, name, sex, account, password, status, disable)
VALUES (1, 'ShuangTian', '男', '2048097948', '$2a$10$7VoeSoVrOvQuBE6QMfLq9O7FLQwEnYrwTpd4k8ldLp4u18nwqaLCa', 1, 1);

-- 2. 系统角色表
CREATE TABLE IF NOT EXISTS role (
                                    role_id INTEGER PRIMARY KEY AUTOINCREMENT, -- 角色ID
                                    role_name TEXT NOT NULL, -- 角色名称
                                    description TEXT NULL, -- 角色描述
                                    UNIQUE (role_name)
);

-- 插入初始角色
INSERT OR IGNORE INTO role (role_id, role_name, description) VALUES (1, 'admin', '管理员');
INSERT OR IGNORE INTO role (role_id, role_name, description) VALUES (2, 'doctor', '医生');
INSERT OR IGNORE INTO role (role_id, role_name, description) VALUES (3, 'patient', '患者');

-- 3. 系统权限表
CREATE TABLE IF NOT EXISTS authority (
                                         authority_id INTEGER PRIMARY KEY AUTOINCREMENT, -- 权限ID
                                         authority_name TEXT NOT NULL, -- 持有权限
                                         description TEXT NULL DEFAULT NULL, -- 持有权限描述
                                         UNIQUE (authority_name)
);

-- 插入初始权限
INSERT OR IGNORE INTO authority (authority_id, authority_name, description) VALUES (1, 'resource:all', '所有权限');
INSERT OR IGNORE INTO authority (authority_id, authority_name, description) VALUES (2, 'resource:get', '获取数据');
INSERT OR IGNORE INTO authority (authority_id, authority_name, description) VALUES (3, 'resource:set', '更改数据');
INSERT OR IGNORE INTO authority (authority_id, authority_name, description) VALUES (4, 'resource:remove', '删除数据');

-- 4. 用户-角色关联表
CREATE TABLE IF NOT EXISTS user_role (
                                         user_id INTEGER NOT NULL, -- 关联sys_user表的用户ID
                                         role_id INTEGER NOT NULL, -- 关联role表的角色ID
                                         PRIMARY KEY (user_id, role_id),
                                         FOREIGN KEY (role_id) REFERENCES role (role_id) ON DELETE CASCADE ON UPDATE CASCADE,
                                         FOREIGN KEY (user_id) REFERENCES sys_user (user_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- 插入初始用户-角色关联
INSERT OR IGNORE INTO user_role (user_id, role_id) VALUES (1, 1);

-- 5. 角色-权限关联表
CREATE TABLE IF NOT EXISTS role_authority (
                                              role_id INTEGER NOT NULL, -- 关联role表的角色ID
                                              authority_id INTEGER NOT NULL, -- 关联authority表的权限ID
                                              PRIMARY KEY (role_id, authority_id),
                                              FOREIGN KEY (authority_id) REFERENCES authority (authority_id) ON DELETE CASCADE ON UPDATE CASCADE,
                                              FOREIGN KEY (role_id) REFERENCES role (role_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- 插入初始角色-权限关联
INSERT OR IGNORE INTO role_authority (role_id, authority_id) VALUES (1, 1);
INSERT OR IGNORE INTO role_authority (role_id, authority_id) VALUES (2, 2);
INSERT OR IGNORE INTO role_authority (role_id, authority_id) VALUES (3, 2);
INSERT OR IGNORE INTO role_authority (role_id, authority_id) VALUES (2, 3);

-- 6. 患者信息表
CREATE TABLE IF NOT EXISTS patient (
                                       patient_id INTEGER PRIMARY KEY AUTOINCREMENT, -- 患者主键ID
                                       name TEXT NOT NULL, -- 患者姓名
                                       gender TEXT NOT NULL CHECK(gender IN ('男','女','未知')), -- 患者性别
                                       birth_date DATE NOT NULL, -- 出生日期
                                       age INTEGER NULL DEFAULT NULL, -- 年龄
                                       id_card_number TEXT NOT NULL, -- 身份证号
                                       contact_number TEXT NULL DEFAULT NULL, -- 联系电话
                                       address TEXT NULL DEFAULT NULL, -- 家庭住址
                                       UNIQUE (id_card_number)
);

-- 7. 患者病历表
CREATE TABLE IF NOT EXISTS medical_record (
                                              record_id INTEGER PRIMARY KEY AUTOINCREMENT, -- 病历ID
                                              patient_id INTEGER NOT NULL, -- 患者ID
                                              visit_date DATETIME NOT NULL, -- 就诊日期
                                              chief_complaint TEXT NULL, -- 主诉
                                              present_illness TEXT NULL, -- 现病史
                                              past_history TEXT NULL, -- 既往史
                                              family_history TEXT NULL, -- 家族史
                                              physical_examination TEXT NULL, -- 体格检查
                                              diagnosis TEXT NULL, -- 诊断
                                              treatment_plan TEXT NULL, -- 治疗计划
                                              discharge_date DATETIME NULL DEFAULT NULL, -- 出院日期
                                              discharge_diagnosis TEXT NULL, -- 出院诊断
                                              discharge_instructions TEXT NULL, -- 出院医嘱
                                              FOREIGN KEY (patient_id) REFERENCES patient (patient_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- 8. 患者用药记录表
CREATE TABLE IF NOT EXISTS medication_record (
                                                 medication_id INTEGER PRIMARY KEY AUTOINCREMENT, -- 用药记录主键ID
                                                 record_id INTEGER NOT NULL, -- 关联病历ID
                                                 drug_name TEXT NOT NULL, -- 药品名称
                                                 dosage TEXT NOT NULL, -- 用药剂量
                                                 frequency TEXT NOT NULL, -- 用药频率
                                                 start_date DATETIME NOT NULL, -- 用药开始日期
                                                 end_date DATETIME NULL DEFAULT NULL, -- 用药结束日期
                                                 FOREIGN KEY (record_id) REFERENCES medical_record (record_id) ON DELETE CASCADE ON UPDATE CASCADE
);

-- 9. 收费记录表
CREATE TABLE IF NOT EXISTS billing_record (
                                              billing_id INTEGER PRIMARY KEY AUTOINCREMENT, -- 收费记录主键ID
                                              record_id INTEGER NOT NULL, -- 关联病历ID
                                              item_name TEXT NOT NULL, -- 收费项目名称
                                              item_cost DECIMAL(10, 2) NOT NULL, -- 项目费用
                                              payment_method TEXT NULL DEFAULT NULL, -- 支付方式
                                              insurance_info TEXT NULL, -- 医保报销信息
                                              FOREIGN KEY (record_id) REFERENCES medical_record (record_id) ON DELETE CASCADE ON UPDATE CASCADE
);