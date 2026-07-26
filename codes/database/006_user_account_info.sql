-- 新增用户头像对象元数据，为账户信息与公开个人中心功能提供 MinIO 头像存储支持。
-- 适用于已经依次执行 001_schema.sql、003_unify_content_type.sql、
-- 004_classroom.sql、005_exam_ai_analysis.sql 的现有数据库。
-- 执行前请备份 learning_platform 数据库；本迁移脚本只应执行一次。

SET NAMES utf8mb4;
SET time_zone = '+08:00';
USE `learning_platform`;

CREATE TABLE `user_avatar` (
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
