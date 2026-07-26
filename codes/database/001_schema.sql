-- 智能在线学习考试平台 MVP
-- MySQL 8.0 schema initialization
-- 安全说明：本脚本不会删除数据库或已有表。

SET NAMES utf8mb4;
SET time_zone = '+08:00';
USE `learning_platform`;

-- =========================================================
-- 1. 用户与角色
-- =========================================================

CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(64) NOT NULL COMMENT '登录用户名',
    `password_hash` VARCHAR(255) NOT NULL COMMENT 'BCrypt密码哈希',
    `nickname` VARCHAR(64) NOT NULL COMMENT '昵称',
    `avatar_url` VARCHAR(512) NULL COMMENT '头像地址',
    `email` VARCHAR(128) NULL COMMENT '邮箱',
    `phone` VARCHAR(32) NULL COMMENT '手机号',
    `gender` VARCHAR(16) NULL COMMENT '性别代码',
    `bio` VARCHAR(500) NULL COMMENT '个人简介',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED/LOCKED',
    `last_login_at` DATETIME(3) NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(64) NULL COMMENT '最后登录IP',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    UNIQUE KEY `uk_user_email` (`email`),
    UNIQUE KEY `uk_user_phone` (`phone`),
    KEY `idx_user_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户';

CREATE TABLE IF NOT EXISTS `user_avatar` (
    `user_id` BIGINT UNSIGNED NOT NULL COMMENT '用户ID，每个用户最多一条当前头像记录',
    `bucket_name` VARCHAR(128) NOT NULL COMMENT 'MinIO bucket 名称',
    `object_name` VARCHAR(512) NOT NULL COMMENT 'MinIO 对象名',
    `original_name` VARCHAR(255) NOT NULL COMMENT '上传时的原始文件名',
    `content_type` VARCHAR(100) NOT NULL COMMENT '图片 MIME 类型',
    `extension` VARCHAR(16) NOT NULL COMMENT '规范化文件扩展名',
    `size_bytes` BIGINT UNSIGNED NOT NULL COMMENT '文件大小（字节）',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_user_avatar_object` (`object_name`),
    CONSTRAINT `chk_user_avatar_size` CHECK (`size_bytes` > 0),
    CONSTRAINT `chk_user_avatar_extension`
        CHECK (`extension` IN ('jpg', 'jpeg', 'png', 'webp')),
    CONSTRAINT `fk_user_avatar_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户当前头像的 MinIO 对象元数据';

CREATE TABLE IF NOT EXISTS `role` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `code` VARCHAR(32) NOT NULL COMMENT 'USER/PUBLISHER/ADMIN',
    `name` VARCHAR(64) NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255) NULL COMMENT '角色说明',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色';

CREATE TABLE IF NOT EXISTS `user_role` (
    `user_id` BIGINT UNSIGNED NOT NULL,
    `role_id` BIGINT UNSIGNED NOT NULL,
    `granted_by` BIGINT UNSIGNED NULL COMMENT '授权管理员ID',
    `granted_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`user_id`, `role_id`),
    KEY `idx_user_role_role` (`role_id`),
    CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_user_role_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关系';

CREATE TABLE IF NOT EXISTS `learning_class` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '班级ID',
    `owner_id` BIGINT UNSIGNED NOT NULL COMMENT '班级拥有者ID',
    `name` VARCHAR(150) NOT NULL COMMENT '班级名称',
    `description` VARCHAR(1000) NULL COMMENT '班级简介',
    `invite_code` VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '唯一班级邀请码',
    `invite_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许通过邀请码加入',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ARCHIVED',
    `version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_learning_class_invite_code` (`invite_code`),
    KEY `idx_learning_class_owner_status` (`owner_id`, `status`, `created_at`),
    CONSTRAINT `fk_learning_class_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级';

CREATE TABLE IF NOT EXISTS `class_member` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `class_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `role` VARCHAR(32) NOT NULL DEFAULT 'MEMBER' COMMENT 'OWNER/ADMIN/MEMBER',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/REMOVED/LEFT',
    `joined_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `left_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_member` (`class_id`, `user_id`),
    KEY `idx_class_member_user_status` (`user_id`, `status`, `joined_at`),
    CONSTRAINT `fk_class_member_class` FOREIGN KEY (`class_id`) REFERENCES `learning_class` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_class_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级成员';

CREATE TABLE IF NOT EXISTS `class_announcement` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `class_id` BIGINT UNSIGNED NOT NULL,
    `author_id` BIGINT UNSIGNED NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `body` LONGTEXT NOT NULL COMMENT 'Markdown 公告正文',
    `pinned` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_class_announcement_class_created` (`class_id`, `pinned`, `created_at`),
    CONSTRAINT `fk_class_announcement_class` FOREIGN KEY (`class_id`) REFERENCES `learning_class` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_class_announcement_author` FOREIGN KEY (`author_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级公告';

-- =========================================================
-- 2. 学习资料与互动
-- =========================================================

CREATE TABLE IF NOT EXISTS `content_category` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `parent_id` BIGINT UNSIGNED NULL COMMENT '父分类ID',
    `name` VARCHAR(100) NOT NULL,
    `slug` VARCHAR(100) NOT NULL COMMENT '分类唯一标识',
    `description` VARCHAR(500) NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_slug` (`slug`),
    KEY `idx_category_parent_sort` (`parent_id`, `sort_order`),
    CONSTRAINT `fk_category_parent` FOREIGN KEY (`parent_id`) REFERENCES `content_category` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习资料分类';

CREATE TABLE IF NOT EXISTS `learning_content` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `publisher_id` BIGINT UNSIGNED NOT NULL COMMENT '发布者ID',
    `category_id` BIGINT UNSIGNED NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `summary` VARCHAR(1000) NULL COMMENT '资料简介',
    `content_type` VARCHAR(32) NOT NULL DEFAULT 'GENERAL' COMMENT '统一资料类型：GENERAL',
    `article_body` LONGTEXT NULL COMMENT '图文正文',
    `cover_file_id` BIGINT UNSIGNED NULL COMMENT '封面文件ID，应用层维护',
    `distribution_mode` VARCHAR(32) NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC/CLASS',
    `is_free` TINYINT(1) NOT NULL DEFAULT 1,
    `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    `rejection_reason` VARCHAR(1000) NULL,
    `view_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `like_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `favorite_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `comment_count` BIGINT UNSIGNED NOT NULL DEFAULT 0,
    `submitted_at` DATETIME(3) NULL,
    `published_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_content_publisher_status` (`publisher_id`, `status`, `created_at`),
    KEY `idx_content_category_status` (`category_id`, `status`, `published_at`),
    KEY `idx_content_free_status` (`is_free`, `status`, `published_at`),
    KEY `idx_content_title` (`title`),
    CONSTRAINT `chk_content_price` CHECK ((`is_free` = 1 AND `price` = 0.00) OR (`is_free` = 0 AND `price` >= 0.01)),
    CONSTRAINT `fk_content_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_content_category` FOREIGN KEY (`category_id`) REFERENCES `content_category` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习资料';

CREATE TABLE IF NOT EXISTS `content_class_scope` (
    `content_id` BIGINT UNSIGNED NULL COMMENT '资料AI任务关联资料；考试AI任务为空',
    `class_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`content_id`, `class_id`),
    KEY `idx_content_class_scope_class` (`class_id`, `content_id`),
    CONSTRAINT `fk_content_class_scope_content` FOREIGN KEY (`content_id`) REFERENCES `learning_content` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_content_class_scope_class` FOREIGN KEY (`class_id`) REFERENCES `learning_class` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习资料班级发放范围';

CREATE TABLE IF NOT EXISTS `content_file` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `content_id` BIGINT UNSIGNED NOT NULL,
    `file_role` VARCHAR(32) NOT NULL COMMENT 'COVER/INLINE_IMAGE/CONTENT/VIDEO/ATTACHMENT/SUBTITLE',
    `original_name` VARCHAR(255) NOT NULL,
    `object_name` VARCHAR(1024) NOT NULL COMMENT 'MinIO对象名',
    `object_name_hash` BINARY(32) GENERATED ALWAYS AS (UNHEX(SHA2(`object_name`, 256))) STORED COMMENT '对象名SHA-256，用于唯一索引',
    `bucket_name` VARCHAR(128) NOT NULL,
    `mime_type` VARCHAR(128) NOT NULL,
    `extension` VARCHAR(32) NULL,
    `size_bytes` BIGINT UNSIGNED NOT NULL,
    `checksum_sha256` CHAR(64) NULL,
    `sort_order` INT NOT NULL DEFAULT 0,
    `duration_seconds` INT UNSIGNED NULL COMMENT '视频时长',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    `uploaded_by` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_content_file_object` (`bucket_name`, `object_name_hash`),
    KEY `idx_content_file_content` (`content_id`, `file_role`, `sort_order`),
    CONSTRAINT `fk_content_file_content` FOREIGN KEY (`content_id`) REFERENCES `learning_content` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_content_file_uploader` FOREIGN KEY (`uploaded_by`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习资料文件';

CREATE TABLE IF NOT EXISTS `learning_progress` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `content_id` BIGINT UNSIGNED NOT NULL,
    `started_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `last_learned_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `progress_percent` DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    `last_position` VARCHAR(255) NULL COMMENT '页码、秒数或章节标识',
    `completed_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_progress_user_content` (`user_id`, `content_id`),
    KEY `idx_progress_user_last` (`user_id`, `last_learned_at`),
    CONSTRAINT `chk_progress_percent` CHECK (`progress_percent` BETWEEN 0.00 AND 100.00),
    CONSTRAINT `fk_progress_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_progress_content` FOREIGN KEY (`content_id`) REFERENCES `learning_content` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习进度';

CREATE TABLE IF NOT EXISTS `content_comment` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `content_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `parent_id` BIGINT UNSIGNED NULL COMMENT '回复的评论ID',
    `body` VARCHAR(2000) NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'VISIBLE',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_comment_content_created` (`content_id`, `status`, `created_at`),
    KEY `idx_comment_user_created` (`user_id`, `created_at`),
    KEY `idx_comment_parent` (`parent_id`),
    CONSTRAINT `fk_comment_content` FOREIGN KEY (`content_id`) REFERENCES `learning_content` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_comment_parent` FOREIGN KEY (`parent_id`) REFERENCES `content_comment` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资料评论';

CREATE TABLE IF NOT EXISTS `content_like` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `content_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_like_user_content` (`user_id`, `content_id`),
    KEY `idx_like_content_created` (`content_id`, `created_at`),
    CONSTRAINT `fk_like_content` FOREIGN KEY (`content_id`) REFERENCES `learning_content` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资料点赞';

CREATE TABLE IF NOT EXISTS `content_favorite` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `content_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_favorite_user_content` (`user_id`, `content_id`),
    KEY `idx_favorite_user_created` (`user_id`, `created_at`),
    CONSTRAINT `fk_favorite_content` FOREIGN KEY (`content_id`) REFERENCES `learning_content` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资料收藏';

-- =========================================================
-- 3. 题库、试卷与考试
-- =========================================================

CREATE TABLE IF NOT EXISTS `question_bank` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `owner_id` BIGINT UNSIGNED NOT NULL COMMENT '发布者ID',
    `name` VARCHAR(150) NOT NULL,
    `description` VARCHAR(1000) NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_bank_owner_status` (`owner_id`, `status`, `created_at`),
    CONSTRAINT `fk_bank_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题库';

CREATE TABLE IF NOT EXISTS `question` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `bank_id` BIGINT UNSIGNED NOT NULL,
    `creator_id` BIGINT UNSIGNED NOT NULL,
    `question_type` VARCHAR(32) NOT NULL,
    `stem` LONGTEXT NOT NULL COMMENT '题干',
    `answer_json` JSON NULL COMMENT '结构化正确答案',
    `answer_text` LONGTEXT NULL COMMENT '参考答案文本',
    `analysis` LONGTEXT NULL COMMENT '答案解析',
    `default_score` DECIMAL(8,2) NOT NULL DEFAULT 1.00,
    `fill_blank_auto_gradable` TINYINT(1) NOT NULL DEFAULT 0,
    `case_sensitive` TINYINT(1) NOT NULL DEFAULT 0,
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_question_bank_type` (`bank_id`, `question_type`, `status`),
    KEY `idx_question_creator` (`creator_id`, `created_at`),
    CONSTRAINT `chk_question_score` CHECK (`default_score` > 0.00),
    CONSTRAINT `fk_question_bank` FOREIGN KEY (`bank_id`) REFERENCES `question_bank` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_question_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目';

CREATE TABLE IF NOT EXISTS `question_option` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `question_id` BIGINT UNSIGNED NOT NULL,
    `option_key` VARCHAR(16) NOT NULL COMMENT 'A/B/C/D或TRUE/FALSE',
    `option_text` TEXT NOT NULL,
    `is_correct` TINYINT(1) NOT NULL DEFAULT 0,
    `sort_order` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_option_key` (`question_id`, `option_key`),
    KEY `idx_option_question_sort` (`question_id`, `sort_order`),
    CONSTRAINT `fk_option_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题目选项';

CREATE TABLE IF NOT EXISTS `exam_paper` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `creator_id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(200) NOT NULL,
    `description` VARCHAR(1000) NULL,
    `total_score` DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    `question_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_paper_creator_status` (`creator_id`, `status`, `created_at`),
    CONSTRAINT `chk_paper_score` CHECK (`total_score` >= 0.00),
    CONSTRAINT `fk_paper_creator` FOREIGN KEY (`creator_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='试卷';

CREATE TABLE IF NOT EXISTS `exam_paper_question` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `paper_id` BIGINT UNSIGNED NOT NULL,
    `question_id` BIGINT UNSIGNED NOT NULL,
    `sort_order` INT NOT NULL,
    `score` DECIMAL(8,2) NOT NULL,
    `question_type_snapshot` VARCHAR(32) NOT NULL,
    `stem_snapshot` LONGTEXT NOT NULL,
    `options_snapshot` JSON NULL,
    `answer_snapshot` JSON NULL COMMENT '仅阅卷接口读取',
    `analysis_snapshot` LONGTEXT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_paper_question` (`paper_id`, `question_id`),
    UNIQUE KEY `uk_paper_sort` (`paper_id`, `sort_order`),
    KEY `idx_paper_question_question` (`question_id`),
    CONSTRAINT `chk_paper_question_score` CHECK (`score` > 0.00),
    CONSTRAINT `fk_paper_question_paper` FOREIGN KEY (`paper_id`) REFERENCES `exam_paper` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_paper_question_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='试卷题目';

CREATE TABLE IF NOT EXISTS `exam` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `publisher_id` BIGINT UNSIGNED NOT NULL,
    `paper_id` BIGINT UNSIGNED NOT NULL,
    `name` VARCHAR(200) NOT NULL,
    `instructions` TEXT NULL,
    `assignment_mode` VARCHAR(32) NOT NULL DEFAULT 'INDIVIDUAL' COMMENT 'INDIVIDUAL/CLASS',
    `start_at` DATETIME(3) NOT NULL,
    `end_at` DATETIME(3) NOT NULL,
    `duration_minutes` INT UNSIGNED NOT NULL,
    `passing_score` DECIMAL(8,2) NOT NULL,
    `show_result_immediately` TINYINT(1) NOT NULL DEFAULT 0,
    `show_answer_after_finish` TINYINT(1) NOT NULL DEFAULT 1,
    `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    `published_at` DATETIME(3) NULL,
    `finished_at` DATETIME(3) NULL,
    `version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_exam_publisher_status` (`publisher_id`, `status`, `start_at`),
    KEY `idx_exam_time_status` (`status`, `start_at`, `end_at`),
    KEY `idx_exam_paper` (`paper_id`),
    CONSTRAINT `chk_exam_time` CHECK (`end_at` > `start_at`),
    CONSTRAINT `chk_exam_duration` CHECK (`duration_minutes` > 0),
    CONSTRAINT `chk_exam_passing_score` CHECK (`passing_score` >= 0.00),
    CONSTRAINT `fk_exam_publisher` FOREIGN KEY (`publisher_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_exam_paper` FOREIGN KEY (`paper_id`) REFERENCES `exam_paper` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考试';

CREATE TABLE IF NOT EXISTS `exam_class_scope` (
    `exam_id` BIGINT UNSIGNED NOT NULL,
    `class_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`exam_id`, `class_id`),
    KEY `idx_exam_class_scope_class` (`class_id`, `exam_id`),
    CONSTRAINT `fk_exam_class_scope_exam` FOREIGN KEY (`exam_id`) REFERENCES `exam` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_exam_class_scope_class` FOREIGN KEY (`class_id`) REFERENCES `learning_class` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考试班级发放范围';

CREATE TABLE IF NOT EXISTS `exam_candidate` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `exam_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'ASSIGNED' COMMENT 'ASSIGNED/STARTED/SUBMITTED/ABSENT',
    `assigned_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `started_at` DATETIME(3) NULL,
    `submitted_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exam_candidate` (`exam_id`, `user_id`),
    KEY `idx_candidate_user_status` (`user_id`, `status`, `assigned_at`),
    CONSTRAINT `fk_candidate_exam` FOREIGN KEY (`exam_id`) REFERENCES `exam` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_candidate_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='指定考生';

CREATE TABLE IF NOT EXISTS `exam_attempt` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `exam_id` BIGINT UNSIGNED NOT NULL,
    `candidate_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `attempt_no` INT UNSIGNED NOT NULL DEFAULT 1,
    `status` VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    `started_at` DATETIME(3) NOT NULL,
    `deadline_at` DATETIME(3) NOT NULL,
    `last_saved_at` DATETIME(3) NULL,
    `submitted_at` DATETIME(3) NULL,
    `submission_type` VARCHAR(32) NULL COMMENT 'MANUAL/TIMEOUT/ADMIN',
    `objective_score` DECIMAL(8,2) NULL,
    `subjective_score` DECIMAL(8,2) NULL,
    `final_score` DECIMAL(8,2) NULL,
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '交卷乐观锁',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_attempt_exam_user_no` (`exam_id`, `user_id`, `attempt_no`),
    KEY `idx_attempt_candidate` (`candidate_id`),
    KEY `idx_attempt_status_deadline` (`status`, `deadline_at`),
    KEY `idx_attempt_user_created` (`user_id`, `created_at`),
    CONSTRAINT `chk_attempt_deadline` CHECK (`deadline_at` > `started_at`),
    CONSTRAINT `fk_attempt_exam` FOREIGN KEY (`exam_id`) REFERENCES `exam` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_attempt_candidate` FOREIGN KEY (`candidate_id`) REFERENCES `exam_candidate` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_attempt_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考试作答记录';

CREATE TABLE IF NOT EXISTS `exam_answer` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `attempt_id` BIGINT UNSIGNED NOT NULL,
    `paper_question_id` BIGINT UNSIGNED NOT NULL,
    `question_id` BIGINT UNSIGNED NOT NULL,
    `answer_json` JSON NULL COMMENT '选择、判断、填空结构化答案',
    `answer_text` LONGTEXT NULL COMMENT '简答等文本答案',
    `max_score` DECIMAL(8,2) NOT NULL,
    `score` DECIMAL(8,2) NULL,
    `is_correct` TINYINT(1) NULL,
    `grading_status` VARCHAR(32) NOT NULL DEFAULT 'UNANSWERED',
    `grader_id` BIGINT UNSIGNED NULL,
    `grader_comment` VARCHAR(2000) NULL,
    `saved_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `graded_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_answer_attempt_question` (`attempt_id`, `question_id`),
    KEY `idx_answer_grading` (`grading_status`, `attempt_id`),
    KEY `idx_answer_paper_question` (`paper_question_id`),
    CONSTRAINT `chk_answer_score` CHECK (`score` IS NULL OR (`score` >= 0.00 AND `score` <= `max_score`)),
    CONSTRAINT `fk_answer_attempt` FOREIGN KEY (`attempt_id`) REFERENCES `exam_attempt` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_answer_paper_question` FOREIGN KEY (`paper_question_id`) REFERENCES `exam_paper_question` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_answer_question` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_answer_grader` FOREIGN KEY (`grader_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考生答案';

CREATE TABLE IF NOT EXISTS `exam_result` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `exam_id` BIGINT UNSIGNED NOT NULL,
    `attempt_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `total_score` DECIMAL(8,2) NOT NULL,
    `passing_score` DECIMAL(8,2) NOT NULL,
    `passed` TINYINT(1) NOT NULL,
    `correct_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `incorrect_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `unanswered_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `grading_completed` TINYINT(1) NOT NULL DEFAULT 0,
    `visible_to_candidate` TINYINT(1) NOT NULL DEFAULT 0,
    `generated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_result_attempt` (`attempt_id`),
    KEY `idx_result_exam_score` (`exam_id`, `total_score`),
    KEY `idx_result_user_created` (`user_id`, `created_at`),
    CONSTRAINT `fk_result_exam` FOREIGN KEY (`exam_id`) REFERENCES `exam` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_result_attempt` FOREIGN KEY (`attempt_id`) REFERENCES `exam_attempt` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_result_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考试成绩';

-- =========================================================
-- 4. AI 学习功能
-- =========================================================

CREATE TABLE IF NOT EXISTS `ai_task` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `request_id` VARCHAR(64) NOT NULL COMMENT '调用幂等号',
    `user_id` BIGINT UNSIGNED NOT NULL,
    `content_id` BIGINT UNSIGNED NULL COMMENT '资料类任务关联资料；考试分析和教师推荐任务为空',
    `conversation_id` BIGINT UNSIGNED NULL COMMENT '会话ID，应用层关联',
    `task_type` VARCHAR(32) NOT NULL
        COMMENT 'SUMMARY/EXPLANATION/EXAM_OVERALL_ANALYSIS/EXAM_PERSONAL_ANALYSIS/OFFLINE_TEACHER_RECOMMENDATION',
    `provider` VARCHAR(32) NOT NULL DEFAULT 'MOCK',
    `model` VARCHAR(100) NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    `input_chars` INT UNSIGNED NOT NULL DEFAULT 0,
    `quota_cost` INT UNSIGNED NOT NULL DEFAULT 1,
    `error_code` VARCHAR(64) NULL,
    `error_message` VARCHAR(1000) NULL,
    `started_at` DATETIME(3) NULL,
    `finished_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_task_request` (`request_id`),
    KEY `idx_ai_task_user_status` (`user_id`, `status`, `created_at`),
    KEY `idx_ai_task_content_type` (`content_id`, `task_type`, `status`),
    CONSTRAINT `fk_ai_task_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ai_task_content` FOREIGN KEY (`content_id`) REFERENCES `learning_content` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI任务';

CREATE TABLE IF NOT EXISTS `ai_summary` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT UNSIGNED NOT NULL,
    `content_id` BIGINT UNSIGNED NOT NULL,
    `summary_text` LONGTEXT NOT NULL,
    `knowledge_points_json` JSON NOT NULL,
    `review_outline` LONGTEXT NOT NULL,
    `source_version` VARCHAR(64) NULL COMMENT '资料文本版本或摘要',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_summary_task` (`task_id`),
    KEY `idx_ai_summary_content_created` (`content_id`, `created_at`),
    CONSTRAINT `fk_ai_summary_task` FOREIGN KEY (`task_id`) REFERENCES `ai_task` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ai_summary_content` FOREIGN KEY (`content_id`) REFERENCES `learning_content` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI资料总结';

CREATE TABLE IF NOT EXISTS `ai_conversation` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `content_id` BIGINT UNSIGNED NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    `last_message_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_conversation_user_last` (`user_id`, `last_message_at`),
    KEY `idx_conversation_content` (`content_id`, `created_at`),
    CONSTRAINT `fk_conversation_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_conversation_content` FOREIGN KEY (`content_id`) REFERENCES `learning_content` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI讲解会话';

CREATE TABLE IF NOT EXISTS `ai_message` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `conversation_id` BIGINT UNSIGNED NOT NULL,
    `task_id` BIGINT UNSIGNED NULL,
    `role` VARCHAR(16) NOT NULL COMMENT 'SYSTEM/USER/ASSISTANT',
    `content` LONGTEXT NOT NULL,
    `sequence_no` INT UNSIGNED NOT NULL,
    `token_count` INT UNSIGNED NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_message_conversation_seq` (`conversation_id`, `sequence_no`),
    KEY `idx_message_task` (`task_id`),
    CONSTRAINT `fk_message_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `ai_conversation` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_message_task` FOREIGN KEY (`task_id`) REFERENCES `ai_task` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI会话消息';

CREATE TABLE IF NOT EXISTS `ai_usage_record` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `business_no` VARCHAR(64) NOT NULL COMMENT '额度操作幂等号',
    `user_id` BIGINT UNSIGNED NOT NULL,
    `task_id` BIGINT UNSIGNED NULL,
    `entitlement_id` BIGINT UNSIGNED NULL COMMENT '权益ID，应用层关联',
    `usage_type` VARCHAR(32) NOT NULL COMMENT 'SUMMARY/EXPLANATION',
    `quantity` INT UNSIGNED NOT NULL,
    `balance_before` INT UNSIGNED NULL,
    `balance_after` INT UNSIGNED NULL,
    `status` VARCHAR(32) NOT NULL COMMENT 'RESERVED/CONSUMED/RELEASED/FAILED',
    `remark` VARCHAR(500) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_usage_business` (`business_no`),
    KEY `idx_ai_usage_user_created` (`user_id`, `created_at`),
    KEY `idx_ai_usage_task` (`task_id`),
    CONSTRAINT `fk_ai_usage_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ai_usage_task` FOREIGN KEY (`task_id`) REFERENCES `ai_task` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI额度使用记录';

CREATE TABLE IF NOT EXISTS `ai_exam_analysis` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT UNSIGNED NOT NULL,
    `exam_id` BIGINT UNSIGNED NOT NULL,
    `attempt_id` BIGINT UNSIGNED NULL COMMENT '个人分析对应作答；整体分析为空',
    `requester_id` BIGINT UNSIGNED NOT NULL,
    `analysis_scope` VARCHAR(32) NOT NULL COMMENT 'OVERALL/PERSONAL',
    `report_markdown` LONGTEXT NOT NULL,
    `input_snapshot_hash` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_exam_analysis_task` (`task_id`),
    KEY `idx_ai_exam_analysis_exam_scope_time` (`exam_id`, `analysis_scope`, `created_at`),
    KEY `idx_ai_exam_analysis_requester_scope_time` (`requester_id`, `analysis_scope`, `created_at`),
    KEY `idx_ai_exam_analysis_attempt_time` (`attempt_id`, `created_at`),
    CONSTRAINT `chk_ai_exam_analysis_scope`
        CHECK (`analysis_scope` IN ('OVERALL', 'PERSONAL')),
    CONSTRAINT `chk_ai_exam_analysis_target`
        CHECK (
            (`analysis_scope` = 'OVERALL' AND `attempt_id` IS NULL)
            OR (`analysis_scope` = 'PERSONAL' AND `attempt_id` IS NOT NULL)
        ),
    CONSTRAINT `fk_ai_exam_analysis_task`
        FOREIGN KEY (`task_id`) REFERENCES `ai_task` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ai_exam_analysis_exam`
        FOREIGN KEY (`exam_id`) REFERENCES `exam` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_ai_exam_analysis_attempt`
        FOREIGN KEY (`attempt_id`) REFERENCES `exam_attempt` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ai_exam_analysis_requester`
        FOREIGN KEY (`requester_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='考试结果AI分析报告';

CREATE TABLE IF NOT EXISTS `offline_teacher_application` (
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
    `availability` VARCHAR(1000) NULL COMMENT '教师可上课的时间范围',
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

CREATE TABLE IF NOT EXISTS `offline_teacher_profile` (
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
    `availability` VARCHAR(1000) NULL COMMENT '教师可上课的时间范围',
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

CREATE TABLE IF NOT EXISTS `offline_student_preference` (
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

CREATE TABLE IF NOT EXISTS `offline_teacher_recommendation` (
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
  COMMENT='线下教师AI推荐结果';

-- =========================================================
-- 5. 商品、订单、支付与权益
-- =========================================================

CREATE TABLE IF NOT EXISTS `product` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `product_code` VARCHAR(64) NOT NULL,
    `product_type` VARCHAR(32) NOT NULL
        COMMENT 'CONTENT/AI_PACKAGE/EXAM_PACKAGE/EXAM_OVERALL_AI_PACKAGE/EXAM_PERSONAL_AI_PACKAGE',
    `name` VARCHAR(200) NOT NULL,
    `description` VARCHAR(1000) NULL,
    `resource_id` BIGINT UNSIGNED NULL COMMENT '付费资料ID',
    `quantity` INT UNSIGNED NULL COMMENT '次数包数量',
    `price` DECIMAL(10,2) NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    `sort_order` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_code` (`product_code`),
    KEY `idx_product_type_status` (`product_type`, `status`, `sort_order`),
    KEY `idx_product_resource` (`product_type`, `resource_id`),
    CONSTRAINT `chk_product_price` CHECK (`price` >= 0.00)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品';

CREATE TABLE IF NOT EXISTS `orders` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_no` VARCHAR(64) NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAYMENT',
    `total_amount` DECIMAL(10,2) NOT NULL,
    `payable_amount` DECIMAL(10,2) NOT NULL,
    `paid_amount` DECIMAL(10,2) NULL,
    `payment_method` VARCHAR(32) NULL COMMENT 'MOCK/其他渠道',
    `remark` VARCHAR(500) NULL,
    `expires_at` DATETIME(3) NULL,
    `paid_at` DATETIME(3) NULL,
    `cancelled_at` DATETIME(3) NULL,
    `version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_orders_order_no` (`order_no`),
    KEY `idx_orders_user_status` (`user_id`, `status`, `created_at`),
    KEY `idx_orders_status_expire` (`status`, `expires_at`),
    CONSTRAINT `chk_orders_amount` CHECK (`total_amount` >= 0.00 AND `payable_amount` >= 0.00),
    CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单';

CREATE TABLE IF NOT EXISTS `order_item` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `order_id` BIGINT UNSIGNED NOT NULL,
    `product_id` BIGINT UNSIGNED NULL COMMENT '商品删除后仍保留快照',
    `product_code_snapshot` VARCHAR(64) NOT NULL,
    `product_type_snapshot` VARCHAR(32) NOT NULL,
    `product_name_snapshot` VARCHAR(200) NOT NULL,
    `resource_id_snapshot` BIGINT UNSIGNED NULL,
    `unit_price` DECIMAL(10,2) NOT NULL,
    `quantity` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '购买件数',
    `entitlement_quantity` INT UNSIGNED NULL COMMENT '每件发放次数',
    `subtotal_amount` DECIMAL(10,2) NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_order_item_order` (`order_id`),
    KEY `idx_order_item_product` (`product_id`),
    CONSTRAINT `chk_order_item_amount` CHECK (`unit_price` >= 0.00 AND `quantity` > 0 AND `subtotal_amount` >= 0.00),
    CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_order_item_product` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单明细';

CREATE TABLE IF NOT EXISTS `payment_record` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `payment_no` VARCHAR(64) NOT NULL,
    `order_id` BIGINT UNSIGNED NOT NULL,
    `provider` VARCHAR(32) NOT NULL DEFAULT 'MOCK',
    `provider_transaction_no` VARCHAR(128) NULL,
    `amount` DECIMAL(10,2) NOT NULL,
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    `request_payload` JSON NULL COMMENT '脱敏后的请求摘要',
    `response_payload` JSON NULL COMMENT '脱敏后的响应摘要',
    `failure_reason` VARCHAR(1000) NULL,
    `paid_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_no` (`payment_no`),
    UNIQUE KEY `uk_payment_provider_txn` (`provider`, `provider_transaction_no`),
    KEY `idx_payment_order_status` (`order_id`, `status`),
    CONSTRAINT `chk_payment_amount` CHECK (`amount` >= 0.00),
    CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='支付记录';

CREATE TABLE IF NOT EXISTS `user_entitlement` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `entitlement_type` VARCHAR(32) NOT NULL
        COMMENT 'CONTENT_ACCESS/AI_QUOTA/EXAM_QUOTA/EXAM_OVERALL_AI_QUOTA/EXAM_PERSONAL_AI_QUOTA',
    `resource_id` BIGINT UNSIGNED NULL COMMENT '资料访问权对应资料ID',
    `source_order_item_id` BIGINT UNSIGNED NULL,
    `total_quantity` INT UNSIGNED NULL COMMENT '次数权益总量，资料访问权为空',
    `available_quantity` INT UNSIGNED NULL COMMENT '次数权益余额',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    `effective_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `expires_at` DATETIME(3) NULL,
    `version` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '额度扣减乐观锁',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_entitlement_order_item` (`source_order_item_id`),
    KEY `idx_entitlement_user_type` (`user_id`, `entitlement_type`, `status`, `expires_at`),
    KEY `idx_entitlement_resource` (`user_id`, `entitlement_type`, `resource_id`, `status`),
    CONSTRAINT `chk_entitlement_quantity` CHECK (
        (`total_quantity` IS NULL AND `available_quantity` IS NULL)
        OR (`total_quantity` IS NOT NULL AND `available_quantity` IS NOT NULL AND `available_quantity` <= `total_quantity`)
    ),
    CONSTRAINT `fk_entitlement_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_entitlement_order_item` FOREIGN KEY (`source_order_item_id`) REFERENCES `order_item` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户权益与额度';

-- =========================================================
-- 6. 管理、审核与系统配置
-- =========================================================

CREATE TABLE IF NOT EXISTS `content_audit` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `content_id` BIGINT UNSIGNED NOT NULL,
    `auditor_id` BIGINT UNSIGNED NULL,
    `action` VARCHAR(32) NOT NULL COMMENT 'SUBMIT/APPROVE/REJECT/OFFLINE/RESTORE',
    `from_status` VARCHAR(32) NULL,
    `to_status` VARCHAR(32) NOT NULL,
    `reason` VARCHAR(1000) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_audit_content_created` (`content_id`, `created_at`),
    KEY `idx_audit_auditor_created` (`auditor_id`, `created_at`),
    CONSTRAINT `fk_audit_content` FOREIGN KEY (`content_id`) REFERENCES `learning_content` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_audit_auditor` FOREIGN KEY (`auditor_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='内容审核记录';

CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `operator_id` BIGINT UNSIGNED NULL,
    `operator_name` VARCHAR(64) NULL COMMENT '历史用户名快照',
    `module` VARCHAR(64) NOT NULL,
    `action` VARCHAR(64) NOT NULL,
    `target_type` VARCHAR(64) NULL,
    `target_id` VARCHAR(64) NULL,
    `request_method` VARCHAR(16) NULL,
    `request_path` VARCHAR(512) NULL,
    `request_id` VARCHAR(64) NULL,
    `ip_address` VARCHAR(64) NULL,
    `user_agent` VARCHAR(1000) NULL,
    `result` VARCHAR(32) NOT NULL COMMENT 'SUCCESS/FAILURE',
    `detail_json` JSON NULL COMMENT '已脱敏操作详情',
    `error_message` VARCHAR(1000) NULL,
    `duration_ms` INT UNSIGNED NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    KEY `idx_operation_operator_created` (`operator_id`, `created_at`),
    KEY `idx_operation_module_created` (`module`, `action`, `created_at`),
    KEY `idx_operation_target` (`target_type`, `target_id`),
    KEY `idx_operation_request_id` (`request_id`),
    CONSTRAINT `fk_operation_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统操作日志';

CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `config_key` VARCHAR(128) NOT NULL,
    `config_value` TEXT NOT NULL,
    `value_type` VARCHAR(32) NOT NULL DEFAULT 'STRING' COMMENT 'STRING/INTEGER/DECIMAL/BOOLEAN/JSON',
    `description` VARCHAR(500) NULL,
    `public_readable` TINYINT(1) NOT NULL DEFAULT 0,
    `updated_by` BIGINT UNSIGNED NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_system_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='非敏感系统配置';
