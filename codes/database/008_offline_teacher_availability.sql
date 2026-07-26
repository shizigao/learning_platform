-- 为线下教师申请及公开教师资料增加可上课时间。
-- 适用于已执行 007_offline_teaching.sql 的现有数据库。
-- 执行前请备份 learning_platform 数据库；本迁移脚本只应执行一次。

SET NAMES utf8mb4;
SET time_zone = '+08:00';
USE `learning_platform`;

ALTER TABLE `offline_teacher_application`
    ADD COLUMN `availability` VARCHAR(1000) NULL
        COMMENT '教师可上课的时间范围' AFTER `teaching_tags`;

ALTER TABLE `offline_teacher_profile`
    ADD COLUMN `availability` VARCHAR(1000) NULL
        COMMENT '教师可上课的时间范围' AFTER `teaching_tags`;
