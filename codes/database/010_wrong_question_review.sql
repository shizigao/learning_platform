-- 新增“错题复习 AI 分析”结果持久化能力。
-- 适用于已经依次执行 001_schema.sql 至 009_seed_offline_teacher_test_data.sql 的数据库。
-- 执行前请备份 learning_platform 数据库；本脚本只应执行一次。

SET NAMES utf8mb4;
SET time_zone = '+08:00';
USE `learning_platform`;

CREATE TABLE `ai_wrong_question_analysis` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT UNSIGNED NOT NULL,
    `requester_id` BIGINT UNSIGNED NOT NULL,
    `exam_count` INT UNSIGNED NOT NULL,
    `question_count` INT UNSIGNED NOT NULL,
    `report_markdown` LONGTEXT NOT NULL,
    `input_snapshot_hash` CHAR(64)
        CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT '生成分析时输入数据的 SHA-256 摘要',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_wrong_question_analysis_task` (`task_id`),
    KEY `idx_ai_wrong_question_analysis_requester_time`
        (`requester_id`, `created_at`),
    CONSTRAINT `chk_ai_wrong_question_analysis_counts`
        CHECK (`exam_count` > 0 AND `question_count` > 0),
    CONSTRAINT `fk_ai_wrong_question_analysis_task`
        FOREIGN KEY (`task_id`) REFERENCES `ai_task` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ai_wrong_question_analysis_requester`
        FOREIGN KEY (`requester_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户最近考试错题的 AI 分析报告';
