CREATE TABLE content_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT NULL,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_test_category_parent FOREIGN KEY (parent_id) REFERENCES content_category (id)
);

CREATE TABLE learning_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    publisher_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(1000) NULL,
    content_type VARCHAR(32) NOT NULL,
    article_body CLOB NULL,
    cover_file_id BIGINT NULL,
    is_free BOOLEAN NOT NULL DEFAULT TRUE,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    rejection_reason VARCHAR(1000) NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    favorite_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    submitted_at TIMESTAMP NULL,
    published_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_test_content_publisher FOREIGN KEY (publisher_id) REFERENCES `user` (id),
    CONSTRAINT fk_test_content_category FOREIGN KEY (category_id) REFERENCES content_category (id)
);

CREATE TABLE content_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT NOT NULL,
    file_role VARCHAR(32) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    object_name VARCHAR(1024) NOT NULL,
    bucket_name VARCHAR(128) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    extension VARCHAR(32) NULL,
    size_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    duration_seconds INT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    uploaded_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_test_file_content FOREIGN KEY (content_id) REFERENCES learning_content (id),
    CONSTRAINT fk_test_file_uploader FOREIGN KEY (uploaded_by) REFERENCES `user` (id)
);

CREATE TABLE user_entitlement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    entitlement_type VARCHAR(32) NOT NULL,
    resource_id BIGINT NULL,
    source_order_item_id BIGINT NULL,
    total_quantity INT NULL,
    available_quantity INT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    effective_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_entitlement_user FOREIGN KEY (user_id) REFERENCES `user` (id)
);

CREATE TABLE learning_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content_id BIGINT NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_learned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    progress_percent DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    last_position VARCHAR(255) NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_test_progress UNIQUE (user_id, content_id),
    CONSTRAINT fk_test_progress_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_test_progress_content FOREIGN KEY (content_id) REFERENCES learning_content (id)
);

CREATE TABLE content_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    body VARCHAR(2000) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'VISIBLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_test_comment_content FOREIGN KEY (content_id) REFERENCES learning_content (id),
    CONSTRAINT fk_test_comment_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_test_comment_parent FOREIGN KEY (parent_id) REFERENCES content_comment (id)
);

CREATE TABLE content_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_test_like UNIQUE (user_id, content_id),
    CONSTRAINT fk_test_like_content FOREIGN KEY (content_id) REFERENCES learning_content (id),
    CONSTRAINT fk_test_like_user FOREIGN KEY (user_id) REFERENCES `user` (id)
);

CREATE TABLE content_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_test_favorite UNIQUE (user_id, content_id),
    CONSTRAINT fk_test_favorite_content FOREIGN KEY (content_id) REFERENCES learning_content (id),
    CONSTRAINT fk_test_favorite_user FOREIGN KEY (user_id) REFERENCES `user` (id)
);

INSERT INTO content_category (name, slug, description, sort_order, enabled)
VALUES
    ('计算机与编程', 'computer-programming', '编程类资料', 10, TRUE),
    ('通识教育', 'general-education', '通识类资料', 20, TRUE);
