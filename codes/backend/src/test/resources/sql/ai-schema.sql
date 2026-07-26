CREATE TABLE ai_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    content_id BIGINT,
    conversation_id BIGINT,
    task_type VARCHAR(32) NOT NULL,
    provider VARCHAR(32) NOT NULL DEFAULT 'MOCK',
    model VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    input_chars INT NOT NULL DEFAULT 0,
    quota_cost INT NOT NULL DEFAULT 1,
    error_code VARCHAR(64),
    error_message VARCHAR(1000),
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_ai_task_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_test_ai_task_content FOREIGN KEY (content_id) REFERENCES learning_content (id)
);

CREATE TABLE ai_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL UNIQUE,
    content_id BIGINT NOT NULL,
    summary_text CLOB NOT NULL,
    knowledge_points_json CLOB NOT NULL,
    review_outline CLOB NOT NULL,
    source_version VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_ai_summary_task FOREIGN KEY (task_id) REFERENCES ai_task (id),
    CONSTRAINT fk_test_ai_summary_content FOREIGN KEY (content_id) REFERENCES learning_content (id)
);

CREATE TABLE ai_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_message_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_test_ai_conversation_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_test_ai_conversation_content FOREIGN KEY (content_id) REFERENCES learning_content (id)
);

CREATE TABLE ai_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    task_id BIGINT,
    role VARCHAR(16) NOT NULL,
    content CLOB NOT NULL,
    sequence_no INT NOT NULL,
    token_count INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_test_ai_message_seq UNIQUE (conversation_id, sequence_no),
    CONSTRAINT fk_test_ai_message_conversation FOREIGN KEY (conversation_id) REFERENCES ai_conversation (id),
    CONSTRAINT fk_test_ai_message_task FOREIGN KEY (task_id) REFERENCES ai_task (id)
);

CREATE TABLE ai_usage_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    task_id BIGINT,
    entitlement_id BIGINT,
    usage_type VARCHAR(32) NOT NULL,
    quantity INT NOT NULL,
    balance_before INT,
    balance_after INT,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_ai_usage_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_test_ai_usage_task FOREIGN KEY (task_id) REFERENCES ai_task (id)
);

CREATE TABLE ai_wrong_question_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL UNIQUE,
    requester_id BIGINT NOT NULL,
    exam_count INT NOT NULL,
    question_count INT NOT NULL,
    report_markdown CLOB NOT NULL,
    input_snapshot_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_wrong_analysis_task
        FOREIGN KEY (task_id) REFERENCES ai_task (id),
    CONSTRAINT fk_test_wrong_analysis_requester
        FOREIGN KEY (requester_id) REFERENCES `user` (id)
);
