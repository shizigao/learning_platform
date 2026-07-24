DROP TABLE IF EXISTS question_option;
DROP TABLE IF EXISTS question;
DROP TABLE IF EXISTS question_bank;

CREATE TABLE question_bank (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_test_bank_owner FOREIGN KEY (owner_id) REFERENCES `user` (id)
);

CREATE TABLE question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_id BIGINT NOT NULL,
    creator_id BIGINT NOT NULL,
    question_type VARCHAR(32) NOT NULL,
    stem VARCHAR(10000) NOT NULL,
    answer_json VARCHAR(20000) NOT NULL,
    answer_text VARCHAR(10000),
    analysis VARCHAR(10000),
    default_score DECIMAL(8, 2) NOT NULL,
    fill_blank_auto_gradable BOOLEAN NOT NULL DEFAULT FALSE,
    case_sensitive BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_test_question_bank FOREIGN KEY (bank_id) REFERENCES question_bank (id),
    CONSTRAINT fk_test_question_creator FOREIGN KEY (creator_id) REFERENCES `user` (id)
);

CREATE TABLE question_option (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    option_key VARCHAR(16) NOT NULL,
    option_text VARCHAR(2000) NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_test_question_option UNIQUE (question_id, option_key),
    CONSTRAINT fk_test_option_question FOREIGN KEY (question_id) REFERENCES question (id)
);
