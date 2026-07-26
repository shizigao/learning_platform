-- 新增考试结果 AI 分析、独立额度商品与分析结果持久化能力。
-- 适用于已经依次执行 001_schema.sql、003_unify_content_type.sql、004_classroom.sql 的现有数据库。
-- 执行前请备份 learning_platform 数据库；本迁移脚本只应执行一次。

SET NAMES utf8mb4;
SET time_zone = '+08:00';
USE `learning_platform`;

-- 考试分析任务不绑定学习资料，因此允许 AI 任务的 content_id 为空。
ALTER TABLE `ai_task`
    MODIFY COLUMN `content_id` BIGINT UNSIGNED NULL;

CREATE TABLE `ai_exam_analysis` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id` BIGINT UNSIGNED NOT NULL,
    `exam_id` BIGINT UNSIGNED NOT NULL,
    `attempt_id` BIGINT UNSIGNED NULL
        COMMENT '个人分析对应的作答记录；整体分析为空',
    `requester_id` BIGINT UNSIGNED NOT NULL,
    `analysis_scope` VARCHAR(32) NOT NULL
        COMMENT 'OVERALL/PERSONAL',
    `report_markdown` LONGTEXT NOT NULL,
    `input_snapshot_hash` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL
        COMMENT '生成分析时输入数据的 SHA-256 摘要',
    `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    `updated_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_exam_analysis_task` (`task_id`),
    KEY `idx_ai_exam_analysis_exam_scope_time`
        (`exam_id`, `analysis_scope`, `created_at`),
    KEY `idx_ai_exam_analysis_requester_scope_time`
        (`requester_id`, `analysis_scope`, `created_at`),
    KEY `idx_ai_exam_analysis_attempt_time`
        (`attempt_id`, `created_at`),
    CONSTRAINT `chk_ai_exam_analysis_scope`
        CHECK (`analysis_scope` IN ('OVERALL', 'PERSONAL')),
    CONSTRAINT `chk_ai_exam_analysis_target`
        CHECK (
            (`analysis_scope` = 'OVERALL' AND `attempt_id` IS NULL)
            OR
            (`analysis_scope` = 'PERSONAL' AND `attempt_id` IS NOT NULL)
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
  COMMENT='考试结果 AI 分析报告';

-- 考试整体 AI 分析额度包（供考试发布者购买）。
INSERT INTO `product`
    (`product_code`, `product_type`, `name`, `description`,
     `quantity`, `price`, `status`, `sort_order`)
VALUES
    ('EXAM_OVERALL_AI_5', 'EXAM_OVERALL_AI_PACKAGE',
     '考试整体AI分析5次包',
     '可生成5次考试整体AI分析报告，生成成功后扣除次数',
     5, 19.90, 'ACTIVE', 50)
ON DUPLICATE KEY UPDATE
    `product_type` = 'EXAM_OVERALL_AI_PACKAGE',
    `name` = '考试整体AI分析5次包',
    `description` = '可生成5次考试整体AI分析报告，生成成功后扣除次数',
    `quantity` = 5,
    `price` = 19.90,
    `status` = 'ACTIVE',
    `sort_order` = 50;

INSERT INTO `product`
    (`product_code`, `product_type`, `name`, `description`,
     `quantity`, `price`, `status`, `sort_order`)
VALUES
    ('EXAM_OVERALL_AI_20', 'EXAM_OVERALL_AI_PACKAGE',
     '考试整体AI分析20次包',
     '可生成20次考试整体AI分析报告，生成成功后扣除次数',
     20, 59.90, 'ACTIVE', 60)
ON DUPLICATE KEY UPDATE
    `product_type` = 'EXAM_OVERALL_AI_PACKAGE',
    `name` = '考试整体AI分析20次包',
    `description` = '可生成20次考试整体AI分析报告，生成成功后扣除次数',
    `quantity` = 20,
    `price` = 59.90,
    `status` = 'ACTIVE',
    `sort_order` = 60;

-- 考试个人 AI 分析额度包（供考生购买）。
INSERT INTO `product`
    (`product_code`, `product_type`, `name`, `description`,
     `quantity`, `price`, `status`, `sort_order`)
VALUES
    ('EXAM_PERSONAL_AI_10', 'EXAM_PERSONAL_AI_PACKAGE',
     '考试个人AI分析10次包',
     '可生成10次考试个人AI分析报告，生成成功后扣除次数',
     10, 9.90, 'ACTIVE', 70)
ON DUPLICATE KEY UPDATE
    `product_type` = 'EXAM_PERSONAL_AI_PACKAGE',
    `name` = '考试个人AI分析10次包',
    `description` = '可生成10次考试个人AI分析报告，生成成功后扣除次数',
    `quantity` = 10,
    `price` = 9.90,
    `status` = 'ACTIVE',
    `sort_order` = 70;

INSERT INTO `product`
    (`product_code`, `product_type`, `name`, `description`,
     `quantity`, `price`, `status`, `sort_order`)
VALUES
    ('EXAM_PERSONAL_AI_50', 'EXAM_PERSONAL_AI_PACKAGE',
     '考试个人AI分析50次包',
     '可生成50次考试个人AI分析报告，生成成功后扣除次数',
     50, 39.90, 'ACTIVE', 80)
ON DUPLICATE KEY UPDATE
    `product_type` = 'EXAM_PERSONAL_AI_PACKAGE',
    `name` = '考试个人AI分析50次包',
    `description` = '可生成50次考试个人AI分析报告，生成成功后扣除次数',
    `quantity` = 50,
    `price` = 39.90,
    `status` = 'ACTIVE',
    `sort_order` = 80;

-- 两类分析分别使用独立额度；仅生成并保存成功时扣除。
INSERT INTO `system_config`
    (`config_key`, `config_value`, `value_type`, `description`, `public_readable`)
VALUES
    ('ai.exam.overall-analysis.quota-cost', '1', 'INTEGER',
     '每次考试整体AI分析成功后扣减次数', 1)
ON DUPLICATE KEY UPDATE
    `config_value` = '1',
    `value_type` = 'INTEGER',
    `description` = '每次考试整体AI分析成功后扣减次数',
    `public_readable` = 1;

INSERT INTO `system_config`
    (`config_key`, `config_value`, `value_type`, `description`, `public_readable`)
VALUES
    ('ai.exam.personal-analysis.quota-cost', '1', 'INTEGER',
     '每次考试个人AI分析成功后扣减次数', 1)
ON DUPLICATE KEY UPDATE
    `config_value` = '1',
    `value_type` = 'INTEGER',
    `description` = '每次考试个人AI分析成功后扣减次数',
    `public_readable` = 1;
