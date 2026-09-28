-- ==============================================
-- 1. 系统用户表（基础表）
-- ==============================================
CREATE TABLE IF NOT EXISTS `sys_user`  (
                                           `user_id` int NOT NULL AUTO_INCREMENT COMMENT '用户ID（自增）',
                                           `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户姓名',
                                           `sex` enum('男','女','未知') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户性别（男/女/未知）',
                                           `account` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录账号（唯一）',
                                           `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '登录密码（Bcrypt加密）',
                                           `status` tinyint(1) NOT NULL COMMENT '用户状态：1=启用，0=禁用',
                                           `disable` tinyint(1) NOT NULL COMMENT '是否禁用',
                                           `ctime` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                           PRIMARY KEY (`user_id`) USING BTREE,
                                           UNIQUE INDEX `uk_account`(`account` ASC) USING BTREE COMMENT '账号唯一索引'
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

-- 插入初始用户（仅当账号不存在时）
INSERT INTO `sys_user` (`user_id`, `name`, `sex`, `account`, `password`, `status`, `disable`)
SELECT 1, 'ShuangTian', '男', '2048097948', '$2a$10$7VoeSoVrOvQuBE6QMfLq9O7FLQwEnYrwTpd4k8ldLp4u18nwqaLCa', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `account` = '2048097948');

-- ==============================================
-- 2. 系统角色表（基础表）
-- ==============================================
CREATE TABLE IF NOT EXISTS `role`  (
                                       `role_id` int NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                                       `role_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
                                       `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '角色描述',
                                       PRIMARY KEY (`role_id`) USING BTREE,
                                       UNIQUE INDEX `uk_role_name`(`role_name` ASC) USING BTREE COMMENT '角色名称唯一，避免重复'
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统角色表' ROW_FORMAT = Dynamic;

-- 插入初始角色（仅当角色名称不存在时）
INSERT INTO `role` (`role_id`, `role_name`, `description`)
SELECT 1, 'admin', '管理员' WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE `role_name` = 'admin');
INSERT INTO `role` (`role_id`, `role_name`, `description`)
SELECT 2, 'doctor', '医生' WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE `role_name` = 'doctor');
INSERT INTO `role` (`role_id`, `role_name`, `description`)
SELECT 3, 'patient', '患者' WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE `role_name` = 'patient');

-- ==============================================
-- 3. 系统权限表（基础表）
-- ==============================================
CREATE TABLE IF NOT EXISTS `authority`  (
                                            `authority_id` int NOT NULL AUTO_INCREMENT COMMENT '权限ID',
                                            `authority_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '持有权限',
                                            `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '持有权限描述',
                                            PRIMARY KEY (`authority_id`) USING BTREE,
                                            UNIQUE INDEX `uk_authority_name`(`authority_name` ASC) USING BTREE COMMENT '权限标识唯一，避免重复'
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统权限表' ROW_FORMAT = Dynamic;

-- 插入初始权限（仅当权限名称不存在时）
INSERT INTO `authority` (`authority_id`, `authority_name`, `description`)
SELECT 1, 'resource:all', '所有权限' WHERE NOT EXISTS (SELECT 1 FROM `authority` WHERE `authority_name` = 'resource:all');
INSERT INTO `authority` (`authority_id`, `authority_name`, `description`)
SELECT 2, 'resource:get', '获取数据' WHERE NOT EXISTS (SELECT 1 FROM `authority` WHERE `authority_name` = 'resource:get');
INSERT INTO `authority` (`authority_id`, `authority_name`, `description`)
SELECT 3, 'resource:set', '更改数据' WHERE NOT EXISTS (SELECT 1 FROM `authority` WHERE `authority_name` = 'resource:set');
INSERT INTO `authority` (`authority_id`, `authority_name`, `description`)
SELECT 4, 'resource:remove', '删除数据' WHERE NOT EXISTS (SELECT 1 FROM `authority` WHERE `authority_name` = 'resource:remove');

-- ==============================================
-- 4. 用户-角色关联表（多对多）
-- ==============================================
CREATE TABLE IF NOT EXISTS `user_role`  (
                                            `user_id` int NOT NULL COMMENT '关联sys_user表的用户ID',
                                            `role_id` int NOT NULL COMMENT '关联role表的角色ID',
                                            PRIMARY KEY (`user_id`, `role_id`) USING BTREE COMMENT '联合主键，避免同一用户重复绑定同一角色',
                                            INDEX `fk_user_role_role`(`role_id` ASC) USING BTREE,
                                            CONSTRAINT `user_role_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON DELETE CASCADE ON UPDATE CASCADE,
                                            CONSTRAINT `user_role_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`user_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户-角色关联表（多对多）' ROW_FORMAT = Dynamic;

-- 插入初始用户-角色关联（仅当关联不存在时）
INSERT INTO `user_role` (`user_id`, `role_id`)
SELECT 1, 1 WHERE NOT EXISTS (SELECT 1 FROM `user_role` WHERE `user_id` = 1 AND `role_id` = 1);

-- ==============================================
-- 5. 角色-权限关联表（多对多）
-- ==============================================
CREATE TABLE IF NOT EXISTS `role_authority`  (
                                                 `role_id` int NOT NULL COMMENT '关联role表的角色ID',
                                                 `authority_id` int NOT NULL COMMENT '关联authority表的权限ID',
                                                 PRIMARY KEY (`role_id`, `authority_id`) USING BTREE COMMENT '联合主键，避免同一角色重复绑定同一权限',
                                                 INDEX `fk_role_auth_auth`(`authority_id` ASC) USING BTREE,
                                                 CONSTRAINT `role_authority_ibfk_1` FOREIGN KEY (`authority_id`) REFERENCES `authority` (`authority_id`) ON DELETE CASCADE ON UPDATE CASCADE,
                                                 CONSTRAINT `role_authority_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `role` (`role_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色-权限关联表（多对多）' ROW_FORMAT = Dynamic;

-- 插入初始角色-权限关联（仅当关联不存在时）
INSERT INTO `role_authority` (`role_id`, `authority_id`)
SELECT 1, 1 WHERE NOT EXISTS (SELECT 1 FROM `role_authority` WHERE `role_id` = 1 AND `authority_id` = 1);
INSERT INTO `role_authority` (`role_id`, `authority_id`)
SELECT 2, 2 WHERE NOT EXISTS (SELECT 1 FROM `role_authority` WHERE `role_id` = 2 AND `authority_id` = 2);
INSERT INTO `role_authority` (`role_id`, `authority_id`)
SELECT 3, 2 WHERE NOT EXISTS (SELECT 1 FROM `role_authority` WHERE `role_id` = 3 AND `authority_id` = 2);
INSERT INTO `role_authority` (`role_id`, `authority_id`)
SELECT 2, 3 WHERE NOT EXISTS (SELECT 1 FROM `role_authority` WHERE `role_id` = 2 AND `authority_id` = 3);

-- ==============================================
-- 6. 患者信息表（基础表）
-- ==============================================
CREATE TABLE IF NOT EXISTS `patient`  (
                                          `patient_id` int NOT NULL AUTO_INCREMENT COMMENT '患者主键ID',
                                          `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '患者姓名',
                                          `gender` enum('男','女','未知') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '患者性别',
                                          `birth_date` date NOT NULL COMMENT '出生日期',
                                          `age` int NULL DEFAULT NULL COMMENT '年龄（可由birth_date计算，也可手动维护）',
                                          `id_card_number` varchar(18) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '身份证号（唯一键）',
                                          `contact_number` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
                                          `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '家庭住址',
                                          PRIMARY KEY (`patient_id`) USING BTREE,
                                          UNIQUE INDEX `uk_id_card`(`id_card_number` ASC) USING BTREE COMMENT '身份证号唯一约束'
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '患者信息表' ROW_FORMAT = Dynamic;

-- ==============================================
-- 7. 患者病历表（依赖patient）
-- ==============================================
CREATE TABLE IF NOT EXISTS `medical_record`  (
                                                 `record_id` int NOT NULL AUTO_INCREMENT COMMENT '病历ID（主键，自增）',
                                                 `patient_id` int NOT NULL COMMENT '患者ID（关联patient表）',
                                                 `visit_date` datetime NOT NULL COMMENT '就诊日期',
                                                 `chief_complaint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '主诉',
                                                 `present_illness` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '现病史',
                                                 `past_history` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '既往史',
                                                 `family_history` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '家族史',
                                                 `physical_examination` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '体格检查',
                                                 `diagnosis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '诊断',
                                                 `treatment_plan` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '治疗计划',
                                                 `discharge_date` datetime NULL DEFAULT NULL COMMENT '出院日期',
                                                 `discharge_diagnosis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '出院诊断',
                                                 `discharge_instructions` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '出院医嘱',
                                                 PRIMARY KEY (`record_id`) USING BTREE,
                                                 INDEX `idx_patient_id`(`patient_id` ASC) USING BTREE,
                                                 CONSTRAINT `medical_record_ibfk_1` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`patient_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '患者病历表' ROW_FORMAT = Dynamic;

-- ==============================================
-- 8. 患者用药记录表（依赖medical_record）
-- ==============================================
CREATE TABLE IF NOT EXISTS `medication_record`  (
                                                    `medication_id` int NOT NULL AUTO_INCREMENT COMMENT '用药记录主键ID',
                                                    `record_id` int NOT NULL COMMENT '关联病历ID',
                                                    `drug_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '药品名称',
                                                    `dosage` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用药剂量（如：500mg/次）',
                                                    `frequency` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用药频率（如：每日3次）',
                                                    `start_date` datetime NOT NULL COMMENT '用药开始日期',
                                                    `end_date` datetime NULL DEFAULT NULL COMMENT '用药结束日期',
                                                    PRIMARY KEY (`medication_id`) USING BTREE,
                                                    INDEX `idx_record_id`(`record_id` ASC) USING BTREE,
                                                    CONSTRAINT `medication_record_ibfk_1` FOREIGN KEY (`record_id`) REFERENCES `medical_record` (`record_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '患者用药记录表' ROW_FORMAT = Dynamic;

-- ==============================================
-- 9. 收费记录表（依赖medical_record）
-- ==============================================
CREATE TABLE IF NOT EXISTS `billing_record`  (
                                                 `billing_id` int NOT NULL AUTO_INCREMENT COMMENT '收费记录主键ID',
                                                 `record_id` int NOT NULL COMMENT '关联病历ID',
                                                 `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收费项目名称',
                                                 `item_cost` decimal(10, 2) NOT NULL COMMENT '项目费用',
                                                 `payment_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付方式（现金/微信/支付宝/医保等）',
                                                 `insurance_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '医保报销信息',
                                                 PRIMARY KEY (`billing_id`) USING BTREE,
                                                 INDEX `idx_record_id`(`record_id` ASC) USING BTREE,
                                                 CONSTRAINT `billing_record_ibfk_1` FOREIGN KEY (`record_id`) REFERENCES `medical_record` (`record_id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收费记录表' ROW_FORMAT = Dynamic;