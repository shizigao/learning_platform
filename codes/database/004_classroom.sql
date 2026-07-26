-- 新增班级、成员、公告，以及学习资料和考试的班级发放能力。
-- 仅适用于已完成 001_schema.sql 和 003_unify_content_type.sql 的现有数据库。
-- 执行前请备份 learning_platform 数据库；本脚本只能执行一次。

SET NAMES utf8mb4;
SET time_zone = '+08:00';
USE `learning_platform`;

CREATE TABLE `learning_class` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `owner_id` BIGINT UNSIGNED NOT NULL COMMENT '班级拥有者用户ID',
    `name` VARCHAR(150) NOT NULL,
    `description` VARCHAR(1000) NULL,
    `invite_code` VARCHAR(16) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT '统一使用大写字母和数字的班级邀请码',
    `invite_enabled` TINYINT(1) NOT NULL DEFAULT 1,
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ARCHIVED',
    `version` INT UNSIGNED NOT NULL DEFAULT 0,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    `deleted` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_learning_class_invite_code` (`invite_code`),
    KEY `idx_learning_class_owner_status` (`owner_id`, `status`, `created_at`),
    CONSTRAINT `chk_learning_class_status`
        CHECK (`status` IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT `fk_learning_class_owner`
        FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级';

CREATE TABLE `class_member` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `class_id` BIGINT UNSIGNED NOT NULL,
    `user_id` BIGINT UNSIGNED NOT NULL,
    `role` VARCHAR(32) NOT NULL DEFAULT 'MEMBER' COMMENT 'OWNER/ADMIN/MEMBER',
    `status` VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/LEFT/REMOVED',
    `joined_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `left_at` DATETIME(3) NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_member` (`class_id`, `user_id`),
    KEY `idx_class_member_user_status` (`user_id`, `status`, `joined_at`),
    KEY `idx_class_member_class_role_status` (`class_id`, `role`, `status`),
    CONSTRAINT `chk_class_member_role`
        CHECK (`role` IN ('OWNER', 'ADMIN', 'MEMBER')),
    CONSTRAINT `chk_class_member_status`
        CHECK (`status` IN ('ACTIVE', 'LEFT', 'REMOVED')),
    CONSTRAINT `fk_class_member_class`
        FOREIGN KEY (`class_id`) REFERENCES `learning_class` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_class_member_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级成员';

CREATE TABLE `class_announcement` (
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
    KEY `idx_announcement_class_pinned_time` (`class_id`, `pinned`, `created_at`),
    KEY `idx_announcement_author` (`author_id`, `created_at`),
    CONSTRAINT `fk_announcement_class`
        FOREIGN KEY (`class_id`) REFERENCES `learning_class` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_announcement_author`
        FOREIGN KEY (`author_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='班级公告';

ALTER TABLE `learning_content`
    ADD COLUMN `distribution_mode` VARCHAR(32) NOT NULL DEFAULT 'PUBLIC'
        COMMENT 'PUBLIC/CLASS' AFTER `cover_file_id`,
    ADD KEY `idx_content_distribution_status`
        (`distribution_mode`, `status`, `published_at`);

CREATE TABLE `content_class_scope` (
    `content_id` BIGINT UNSIGNED NOT NULL,
    `class_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`content_id`, `class_id`),
    KEY `idx_content_class_scope_class` (`class_id`, `created_at`),
    CONSTRAINT `fk_content_class_scope_content`
        FOREIGN KEY (`content_id`) REFERENCES `learning_content` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_content_class_scope_class`
        FOREIGN KEY (`class_id`) REFERENCES `learning_class` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学习资料班级发放范围';

ALTER TABLE `exam`
    ADD COLUMN `assignment_mode` VARCHAR(32) NOT NULL DEFAULT 'INDIVIDUAL'
        COMMENT 'INDIVIDUAL/CLASS' AFTER `instructions`,
    ADD KEY `idx_exam_assignment_status`
        (`assignment_mode`, `status`, `start_at`, `end_at`);

CREATE TABLE `exam_class_scope` (
    `exam_id` BIGINT UNSIGNED NOT NULL,
    `class_id` BIGINT UNSIGNED NOT NULL,
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`exam_id`, `class_id`),
    KEY `idx_exam_class_scope_class` (`class_id`, `created_at`),
    CONSTRAINT `fk_exam_class_scope_exam`
        FOREIGN KEY (`exam_id`) REFERENCES `exam` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_exam_class_scope_class`
        FOREIGN KEY (`class_id`) REFERENCES `learning_class` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考试班级发放范围';

-- 历史资料和考试在新增字段时已通过默认值保持原有行为。
SELECT `distribution_mode`, COUNT(*) AS `content_count`
FROM `learning_content`
GROUP BY `distribution_mode`;

SELECT `assignment_mode`, COUNT(*) AS `exam_count`
FROM `exam`
GROUP BY `assignment_mode`;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'learning_platform'
  AND table_name IN (
      'learning_class',
      'class_member',
      'class_announcement',
      'content_class_scope',
      'exam_class_scope'
  )
ORDER BY table_name;
