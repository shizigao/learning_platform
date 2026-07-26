-- 新增线下教师申请、公开教师资料、学生匹配偏好与 AI 推荐结果。
-- 适用于已经依次执行 001_schema.sql、003_unify_content_type.sql、
-- 004_classroom.sql、005_exam_ai_analysis.sql、006_user_account_info.sql 的现有数据库。
-- 执行前请备份 learning_platform 数据库；本迁移脚本只应执行一次。

SET NAMES utf8mb4;
SET time_zone = '+08:00';
USE `learning_platform`;

CREATE TABLE `offline_teacher_application` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '申请人用户ID，每名用户保留一份当前申请',
    `teacher_name` VARCHAR(64) NOT NULL COMMENT '教师真实姓名',
    `id_card_ciphertext` VARBINARY(512) NOT NULL COMMENT 'AES-256-GCM 加密后的身份证号及认证标签',
    `id_card_iv` BINARY(12) NOT NULL COMMENT '身份证号加密随机 IV',
    `id_card_hmac` BINARY(32) NOT NULL COMMENT '身份证号 HMAC-SHA-256，用于防重复申请',
    `id_card_masked` VARCHAR(32) NOT NULL COMMENT '仅供普通展示的脱敏身份证号',
    `gender` VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'UNKNOWN/MALE/FEMALE',
    `education_level` VARCHAR(32) NOT NULL COMMENT '学历层级',
    `education_background` VARCHAR(1000) NOT NULL COMMENT '教育背景',
    `institution` VARCHAR(200) NULL COMMENT '所属机构',
    `province` VARCHAR(100) NOT NULL,
    `city` VARCHAR(100) NOT NULL,
    `district` VARCHAR(100) NULL,
    `bio` VARCHAR(2000) NOT NULL COMMENT '教师个人简介',
    `teaching_content` VARCHAR(2000) NOT NULL COMMENT '教授内容',
    `teaching_tags` JSON NOT NULL COMMENT '规范化教学标签数组',
    `hourly_rate` DECIMAL(10,2) NOT NULL COMMENT '每课时价格',
    `price_description` VARCHAR(500) NULL,
    `contact_wechat` VARCHAR(100) NULL,
    `contact_qq` VARCHAR(32) NULL,
    `contact_email` VARCHAR(128) NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT'
        COMMENT 'DRAFT/PENDING/APPROVED/REJECTED/WITHDRAWN',
    `rejection_reason` VARCHAR(1000) NULL,
    `submitted_at` DATETIME(3) NULL,
    `reviewed_at` DATETIME(3) NULL,
    `reviewed_by` BIGINT UNSIGNED NULL,
    `version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_offline_teacher_application_user` (`user_id`),
    UNIQUE KEY `uk_offline_teacher_application_id_card` (`id_card_hmac`),
    KEY `idx_offline_teacher_application_status_time`
        (`status`, `submitted_at`, `id`),
    KEY `idx_offline_teacher_application_reviewer`
        (`reviewed_by`, `reviewed_at`),
    CONSTRAINT `chk_offline_teacher_application_gender`
        CHECK (`gender` IN ('UNKNOWN', 'MALE', 'FEMALE')),
    CONSTRAINT `chk_offline_teacher_application_education`
        CHECK (`education_level` IN (
            'HIGH_SCHOOL', 'ASSOCIATE', 'BACHELOR', 'MASTER', 'DOCTOR', 'OTHER'
        )),
    CONSTRAINT `chk_offline_teacher_application_status`
        CHECK (`status` IN (
            'DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'WITHDRAWN'
        )),
    CONSTRAINT `chk_offline_teacher_application_rate`
        CHECK (`hourly_rate` >= 0.01),
    CONSTRAINT `chk_offline_teacher_application_contact`
        CHECK (
            `contact_wechat` IS NOT NULL
            OR `contact_qq` IS NOT NULL
            OR `contact_email` IS NOT NULL
        ),
    CONSTRAINT `fk_offline_teacher_application_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_offline_teacher_application_reviewer`
        FOREIGN KEY (`reviewed_by`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='线下教师当前申请与审核状态';

CREATE TABLE `offline_teacher_profile` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `source_application_id` BIGINT UNSIGNED NOT NULL,
    `teacher_name` VARCHAR(64) NOT NULL,
    `gender` VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
    `education_level` VARCHAR(32) NOT NULL,
    `education_background` VARCHAR(1000) NOT NULL,
    `institution` VARCHAR(200) NULL,
    `province` VARCHAR(100) NOT NULL,
    `city` VARCHAR(100) NOT NULL,
    `district` VARCHAR(100) NULL,
    `bio` VARCHAR(2000) NOT NULL,
    `teaching_content` VARCHAR(2000) NOT NULL,
    `teaching_tags` JSON NOT NULL,
    `hourly_rate` DECIMAL(10,2) NOT NULL,
    `price_description` VARCHAR(500) NULL,
    `contact_wechat` VARCHAR(100) NULL,
    `contact_qq` VARCHAR(32) NULL,
    `contact_email` VARCHAR(128) NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/SUSPENDED',
    `suspended_reason` VARCHAR(1000) NULL,
    `approved_at` DATETIME(3) NOT NULL,
    `approved_by` BIGINT UNSIGNED NOT NULL,
    `version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_offline_teacher_profile_user` (`user_id`),
    UNIQUE KEY `uk_offline_teacher_profile_application` (`source_application_id`),
    KEY `idx_offline_teacher_profile_search`
        (`status`, `province`, `city`, `hourly_rate`, `id`),
    KEY `idx_offline_teacher_profile_institution`
        (`institution`, `status`),
    CONSTRAINT `chk_offline_teacher_profile_gender`
        CHECK (`gender` IN ('UNKNOWN', 'MALE', 'FEMALE')),
    CONSTRAINT `chk_offline_teacher_profile_education`
        CHECK (`education_level` IN (
            'HIGH_SCHOOL', 'ASSOCIATE', 'BACHELOR', 'MASTER', 'DOCTOR', 'OTHER'
        )),
    CONSTRAINT `chk_offline_teacher_profile_status`
        CHECK (`status` IN ('ACTIVE', 'SUSPENDED')),
    CONSTRAINT `chk_offline_teacher_profile_rate`
        CHECK (`hourly_rate` >= 0.01),
    CONSTRAINT `chk_offline_teacher_profile_contact`
        CHECK (
            `contact_wechat` IS NOT NULL
            OR `contact_qq` IS NOT NULL
            OR `contact_email` IS NOT NULL
        ),
    CONSTRAINT `fk_offline_teacher_profile_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_offline_teacher_profile_application`
        FOREIGN KEY (`source_application_id`)
        REFERENCES `offline_teacher_application` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_offline_teacher_profile_approver`
        FOREIGN KEY (`approved_by`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='审核通过后面向学生公开的线下教师资料';

CREATE TABLE `offline_student_preference` (
    `user_id` BIGINT UNSIGNED NOT NULL,
    `subject` VARCHAR(200) NOT NULL COMMENT '希望学习的科目或内容',
    `current_level` VARCHAR(500) NOT NULL,
    `learning_goals` VARCHAR(2000) NOT NULL,
    `weaknesses` VARCHAR(2000) NULL,
    `province` VARCHAR(100) NOT NULL,
    `city` VARCHAR(100) NOT NULL,
    `district` VARCHAR(100) NULL,
    `max_hourly_rate` DECIMAL(10,2) NULL,
    `availability` VARCHAR(1000) NULL,
    `teacher_preferences` VARCHAR(2000) NULL,
    `additional_notes` VARCHAR(2000) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`user_id`),
    CONSTRAINT `chk_offline_student_preference_rate`
        CHECK (`max_hourly_rate` IS NULL OR `max_hourly_rate` >= 0.01),
    CONSTRAINT `fk_offline_student_preference_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='学生线下教师匹配偏好';

CREATE TABLE `offline_teacher_recommendation` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `preference_snapshot` JSON NOT NULL COMMENT '生成时的学生偏好快照',
    `candidate_snapshot` JSON NOT NULL COMMENT '本地算法选出的候选教师安全信息',
    `recommendation_json` JSON NOT NULL COMMENT '已校验的推荐教师ID、理由与匹配说明',
    `input_snapshot_hash` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_offline_teacher_recommendation_task` (`task_id`),
    KEY `idx_offline_teacher_recommendation_user_time`
        (`user_id`, `created_at`, `id`),
    CONSTRAINT `fk_offline_teacher_recommendation_task`
        FOREIGN KEY (`task_id`) REFERENCES `ai_task` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_offline_teacher_recommendation_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='线下教师 AI 推荐结果';
