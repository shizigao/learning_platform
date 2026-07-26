-- 将历史学习资料的五类资料类型统一为 GENERAL。
-- 执行前请备份 learning_platform 数据库。

SET NAMES utf8mb4;
SET time_zone = '+08:00';
USE `learning_platform`;

ALTER TABLE `learning_content`
    MODIFY COLUMN `content_type` VARCHAR(32) NOT NULL DEFAULT 'GENERAL'
        COMMENT '统一资料类型：GENERAL';

START TRANSACTION;

UPDATE `learning_content`
SET `content_type` = 'GENERAL'
WHERE `content_type` <> 'GENERAL';

COMMIT;

SELECT `content_type`, COUNT(*) AS `content_count`
FROM `learning_content`
WHERE `deleted` = 0
GROUP BY `content_type`;
