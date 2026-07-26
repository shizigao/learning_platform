DROP TABLE IF EXISTS offline_teacher_recommendation;
DROP TABLE IF EXISTS offline_student_preference;
DROP TABLE IF EXISTS offline_teacher_profile;
DROP TABLE IF EXISTS offline_teacher_application;
DROP TABLE IF EXISTS ai_wrong_question_analysis;
DROP TABLE IF EXISTS ai_message;
DROP TABLE IF EXISTS ai_usage_record;
DROP TABLE IF EXISTS ai_summary;
DROP TABLE IF EXISTS ai_conversation;
DROP TABLE IF EXISTS ai_task;
DROP TABLE IF EXISTS exam_result;
DROP TABLE IF EXISTS exam_answer;
DROP TABLE IF EXISTS exam_attempt;
DROP TABLE IF EXISTS exam_candidate;
DROP TABLE IF EXISTS exam_class_scope;
DROP TABLE IF EXISTS exam;
DROP TABLE IF EXISTS exam_paper_question;
DROP TABLE IF EXISTS exam_paper;
DROP TABLE IF EXISTS payment_record;
DROP TABLE IF EXISTS user_entitlement;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS question_option;
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS question_bank;
DROP TABLE IF EXISTS content_comment;
DROP TABLE IF EXISTS content_like;
DROP TABLE IF EXISTS content_favorite;
DROP TABLE IF EXISTS learning_progress;
DROP TABLE IF EXISTS content_file;
DROP TABLE IF EXISTS content_class_scope;
DROP TABLE IF EXISTS learning_content;
DROP TABLE IF EXISTS content_category;
DROP TABLE IF EXISTS class_announcement;
DROP TABLE IF EXISTS class_member;
DROP TABLE IF EXISTS learning_class;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS user_avatar;
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    avatar_url VARCHAR(512),
    email VARCHAR(128) UNIQUE,
    phone VARCHAR(32) UNIQUE,
    gender VARCHAR(16),
    bio VARCHAR(500),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP,
    last_login_ip VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE user_avatar (
    user_id BIGINT PRIMARY KEY,
    bucket_name VARCHAR(128) NOT NULL,
    object_name VARCHAR(512) NOT NULL UNIQUE,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    extension VARCHAR(16) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_avatar_user
        FOREIGN KEY (user_id) REFERENCES `user` (id) ON DELETE CASCADE
);

CREATE TABLE role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    granted_by BIGINT,
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_test_user_role_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_test_user_role_role FOREIGN KEY (role_id) REFERENCES role (id)
);

CREATE TABLE operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT,
    operator_name VARCHAR(64),
    module VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64),
    target_id VARCHAR(64),
    request_method VARCHAR(16),
    request_path VARCHAR(512),
    request_id VARCHAR(64),
    ip_address VARCHAR(64),
    user_agent VARCHAR(1000),
    result VARCHAR(32) NOT NULL,
    detail_json VARCHAR(2000),
    error_message VARCHAR(1000),
    duration_ms INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_operation_operator
        FOREIGN KEY (operator_id) REFERENCES `user` (id)
);

INSERT INTO role (code, name, description, enabled)
VALUES
    ('USER', '普通用户', '普通学习用户', TRUE),
    ('PUBLISHER', '发布者', '学习内容发布者', TRUE),
    ('ADMIN', '管理员', '系统管理员', TRUE);
