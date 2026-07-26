-- 智能在线学习考试平台 MVP 初始数据
-- 本脚本不包含管理员密码、数据库密码或第三方密钥，可重复执行。

SET NAMES utf8mb4;
SET time_zone = '+08:00';
USE `learning_platform`;

START TRANSACTION;

-- 系统角色
INSERT INTO `role` (`code`, `name`, `description`, `enabled`)
VALUES ('USER', '普通用户', '浏览和学习资料、参加考试、使用AI学习功能', 1)
ON DUPLICATE KEY UPDATE
    `name` = '普通用户',
    `description` = '浏览和学习资料、参加考试、使用AI学习功能',
    `enabled` = 1;

INSERT INTO `role` (`code`, `name`, `description`, `enabled`)
VALUES ('PUBLISHER', '发布者', '发布学习资料、维护题库试卷、发布考试和阅卷', 1)
ON DUPLICATE KEY UPDATE
    `name` = '发布者',
    `description` = '发布学习资料、维护题库试卷、发布考试和阅卷',
    `enabled` = 1;

INSERT INTO `role` (`code`, `name`, `description`, `enabled`)
VALUES ('ADMIN', '管理员', '管理用户、审核内容、管理订单配置并查看日志', 1)
ON DUPLICATE KEY UPDATE
    `name` = '管理员',
    `description` = '管理用户、审核内容、管理订单配置并查看日志',
    `enabled` = 1;

-- 初始资料分类
INSERT INTO `content_category` (`parent_id`, `name`, `slug`, `description`, `sort_order`, `enabled`)
VALUES (NULL, '计算机与编程', 'computer-programming', '编程语言、软件开发和计算机基础', 10, 1)
ON DUPLICATE KEY UPDATE
    `name` = '计算机与编程',
    `description` = '编程语言、软件开发和计算机基础',
    `sort_order` = 10,
    `enabled` = 1;

INSERT INTO `content_category` (`parent_id`, `name`, `slug`, `description`, `sort_order`, `enabled`)
VALUES (NULL, '通识教育', 'general-education', '语言、人文、科学等通识学习内容', 20, 1)
ON DUPLICATE KEY UPDATE
    `name` = '通识教育',
    `description` = '语言、人文、科学等通识学习内容',
    `sort_order` = 20,
    `enabled` = 1;

INSERT INTO `content_category` (`parent_id`, `name`, `slug`, `description`, `sort_order`, `enabled`)
VALUES (NULL, '职业技能', 'professional-skills', '职业发展和实用技能学习内容', 30, 1)
ON DUPLICATE KEY UPDATE
    `name` = '职业技能',
    `description` = '职业发展和实用技能学习内容',
    `sort_order` = 30,
    `enabled` = 1;

-- AI 次数包
INSERT INTO `product` (`product_code`, `product_type`, `name`, `description`, `quantity`, `price`, `status`, `sort_order`)
VALUES ('AI_PACKAGE_10', 'AI_PACKAGE', 'AI学习助手10次包', '可用于资料总结或知识讲解，共10次', 10, 9.90, 'ACTIVE', 10)
ON DUPLICATE KEY UPDATE
    `name` = 'AI学习助手10次包',
    `description` = '可用于资料总结或知识讲解，共10次',
    `quantity` = 10,
    `price` = 9.90,
    `status` = 'ACTIVE',
    `sort_order` = 10;

INSERT INTO `product` (`product_code`, `product_type`, `name`, `description`, `quantity`, `price`, `status`, `sort_order`)
VALUES ('AI_PACKAGE_50', 'AI_PACKAGE', 'AI学习助手50次包', '可用于资料总结或知识讲解，共50次', 50, 39.90, 'ACTIVE', 20)
ON DUPLICATE KEY UPDATE
    `name` = 'AI学习助手50次包',
    `description` = '可用于资料总结或知识讲解，共50次',
    `quantity` = 50,
    `price` = 39.90,
    `status` = 'ACTIVE',
    `sort_order` = 20;

-- 考试发布次数包
INSERT INTO `product` (`product_code`, `product_type`, `name`, `description`, `quantity`, `price`, `status`, `sort_order`)
VALUES ('EXAM_PACKAGE_5', 'EXAM_PACKAGE', '考试发布5次包', '发布者可发布5场考试', 5, 19.90, 'ACTIVE', 30)
ON DUPLICATE KEY UPDATE
    `name` = '考试发布5次包',
    `description` = '发布者可发布5场考试',
    `quantity` = 5,
    `price` = 19.90,
    `status` = 'ACTIVE',
    `sort_order` = 30;

INSERT INTO `product` (`product_code`, `product_type`, `name`, `description`, `quantity`, `price`, `status`, `sort_order`)
VALUES ('EXAM_PACKAGE_20', 'EXAM_PACKAGE', '考试发布20次包', '发布者可发布20场考试', 20, 59.90, 'ACTIVE', 40)
ON DUPLICATE KEY UPDATE
    `name` = '考试发布20次包',
    `description` = '发布者可发布20场考试',
    `quantity` = 20,
    `price` = 59.90,
    `status` = 'ACTIVE',
    `sort_order` = 40;

-- 考试结果 AI 分析次数包
INSERT INTO `product` (`product_code`, `product_type`, `name`, `description`, `quantity`, `price`, `status`, `sort_order`)
VALUES ('EXAM_OVERALL_AI_5', 'EXAM_OVERALL_AI_PACKAGE', '考试整体AI分析5次包', '可生成5次考试整体AI分析报告，生成成功后扣除次数', 5, 19.90, 'ACTIVE', 50)
ON DUPLICATE KEY UPDATE
    `product_type` = 'EXAM_OVERALL_AI_PACKAGE',
    `name` = '考试整体AI分析5次包',
    `description` = '可生成5次考试整体AI分析报告，生成成功后扣除次数',
    `quantity` = 5, `price` = 19.90, `status` = 'ACTIVE', `sort_order` = 50;

INSERT INTO `product` (`product_code`, `product_type`, `name`, `description`, `quantity`, `price`, `status`, `sort_order`)
VALUES ('EXAM_OVERALL_AI_20', 'EXAM_OVERALL_AI_PACKAGE', '考试整体AI分析20次包', '可生成20次考试整体AI分析报告，生成成功后扣除次数', 20, 59.90, 'ACTIVE', 60)
ON DUPLICATE KEY UPDATE
    `product_type` = 'EXAM_OVERALL_AI_PACKAGE',
    `name` = '考试整体AI分析20次包',
    `description` = '可生成20次考试整体AI分析报告，生成成功后扣除次数',
    `quantity` = 20, `price` = 59.90, `status` = 'ACTIVE', `sort_order` = 60;

INSERT INTO `product` (`product_code`, `product_type`, `name`, `description`, `quantity`, `price`, `status`, `sort_order`)
VALUES ('EXAM_PERSONAL_AI_10', 'EXAM_PERSONAL_AI_PACKAGE', '考试个人AI分析10次包', '可生成10次考试个人AI分析报告，生成成功后扣除次数', 10, 9.90, 'ACTIVE', 70)
ON DUPLICATE KEY UPDATE
    `product_type` = 'EXAM_PERSONAL_AI_PACKAGE',
    `name` = '考试个人AI分析10次包',
    `description` = '可生成10次考试个人AI分析报告，生成成功后扣除次数',
    `quantity` = 10, `price` = 9.90, `status` = 'ACTIVE', `sort_order` = 70;

INSERT INTO `product` (`product_code`, `product_type`, `name`, `description`, `quantity`, `price`, `status`, `sort_order`)
VALUES ('EXAM_PERSONAL_AI_50', 'EXAM_PERSONAL_AI_PACKAGE', '考试个人AI分析50次包', '可生成50次考试个人AI分析报告，生成成功后扣除次数', 50, 39.90, 'ACTIVE', 80)
ON DUPLICATE KEY UPDATE
    `product_type` = 'EXAM_PERSONAL_AI_PACKAGE',
    `name` = '考试个人AI分析50次包',
    `description` = '可生成50次考试个人AI分析报告，生成成功后扣除次数',
    `quantity` = 50, `price` = 39.90, `status` = 'ACTIVE', `sort_order` = 80;

-- 非敏感系统配置。DeepSeek API Key 必须只通过环境变量提供。
INSERT INTO `system_config` (`config_key`, `config_value`, `value_type`, `description`, `public_readable`)
VALUES ('ai.enabled', 'true', 'BOOLEAN', '是否启用AI学习功能', 0)
ON DUPLICATE KEY UPDATE `config_value` = 'true', `value_type` = 'BOOLEAN', `description` = '是否启用AI学习功能';

INSERT INTO `system_config` (`config_key`, `config_value`, `value_type`, `description`, `public_readable`)
VALUES ('ai.provider', 'deepseek', 'STRING', 'AI服务供应商代码，不包含密钥', 0)
ON DUPLICATE KEY UPDATE `config_value` = 'deepseek', `value_type` = 'STRING', `description` = 'AI服务供应商代码，不包含密钥';

INSERT INTO `system_config` (`config_key`, `config_value`, `value_type`, `description`, `public_readable`)
VALUES ('ai.model', 'deepseek-v4-flash', 'STRING', '默认DeepSeek模型，可由环境变量覆盖', 0)
ON DUPLICATE KEY UPDATE `config_value` = 'deepseek-v4-flash', `value_type` = 'STRING', `description` = '默认DeepSeek模型，可由环境变量覆盖';

INSERT INTO `system_config` (`config_key`, `config_value`, `value_type`, `description`, `public_readable`)
VALUES ('ai.summary.quota_cost', '1', 'INTEGER', '每次资料总结扣减次数', 1)
ON DUPLICATE KEY UPDATE `config_value` = '1', `value_type` = 'INTEGER', `description` = '每次资料总结扣减次数', `public_readable` = 1;

INSERT INTO `system_config` (`config_key`, `config_value`, `value_type`, `description`, `public_readable`)
VALUES ('ai.explanation.quota_cost', '1', 'INTEGER', '每次知识讲解扣减次数', 1)
ON DUPLICATE KEY UPDATE `config_value` = '1', `value_type` = 'INTEGER', `description` = '每次知识讲解扣减次数', `public_readable` = 1;

INSERT INTO `system_config` (`config_key`, `config_value`, `value_type`, `description`, `public_readable`)
VALUES ('exam.publish.quota_cost', '1', 'INTEGER', '每发布一场考试扣减次数', 1)
ON DUPLICATE KEY UPDATE `config_value` = '1', `value_type` = 'INTEGER', `description` = '每发布一场考试扣减次数', `public_readable` = 1;

INSERT INTO `system_config` (`config_key`, `config_value`, `value_type`, `description`, `public_readable`)
VALUES ('ai.exam.overall-analysis.quota-cost', '1', 'INTEGER', '每次考试整体AI分析成功后扣减次数', 1)
ON DUPLICATE KEY UPDATE `config_value` = '1', `value_type` = 'INTEGER', `description` = '每次考试整体AI分析成功后扣减次数', `public_readable` = 1;

INSERT INTO `system_config` (`config_key`, `config_value`, `value_type`, `description`, `public_readable`)
VALUES ('ai.exam.personal-analysis.quota-cost', '1', 'INTEGER', '每次考试个人AI分析成功后扣减次数', 1)
ON DUPLICATE KEY UPDATE `config_value` = '1', `value_type` = 'INTEGER', `description` = '每次考试个人AI分析成功后扣减次数', `public_readable` = 1;

INSERT INTO `system_config` (`config_key`, `config_value`, `value_type`, `description`, `public_readable`)
VALUES ('payment.mock.enabled', 'true', 'BOOLEAN', 'MVP是否启用模拟支付', 1)
ON DUPLICATE KEY UPDATE `config_value` = 'true', `value_type` = 'BOOLEAN', `description` = 'MVP是否启用模拟支付', `public_readable` = 1;

INSERT INTO `system_config` (`config_key`, `config_value`, `value_type`, `description`, `public_readable`)
VALUES ('upload.max_file_size_mb', '200', 'INTEGER', '单文件最大上传大小MB', 1)
ON DUPLICATE KEY UPDATE `config_value` = '200', `value_type` = 'INTEGER', `description` = '单文件最大上传大小MB', `public_readable` = 1;

COMMIT;
