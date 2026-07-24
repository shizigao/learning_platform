DROP TABLE IF EXISTS exam_answer;
DROP TABLE IF EXISTS exam_attempt;
DROP TABLE IF EXISTS exam_candidate;
DROP TABLE IF EXISTS exam;
DROP TABLE IF EXISTS exam_paper_question;
DROP TABLE IF EXISTS exam_paper;
DROP TABLE IF EXISTS user_entitlement;

CREATE TABLE exam_paper (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    creator_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    total_score DECIMAL(8, 2) NOT NULL DEFAULT 0,
    question_count INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_test_paper_creator FOREIGN KEY (creator_id) REFERENCES `user` (id)
);

CREATE TABLE exam_paper_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    paper_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    score DECIMAL(8, 2) NOT NULL,
    question_type_snapshot VARCHAR(32) NOT NULL,
    stem_snapshot VARCHAR(10000) NOT NULL,
    options_snapshot VARCHAR(20000),
    answer_snapshot VARCHAR(20000),
    analysis_snapshot VARCHAR(10000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_test_paper_question UNIQUE (paper_id, question_id),
    CONSTRAINT uk_test_paper_sort UNIQUE (paper_id, sort_order),
    CONSTRAINT fk_test_paper_question_paper FOREIGN KEY (paper_id) REFERENCES exam_paper (id),
    CONSTRAINT fk_test_paper_question_question FOREIGN KEY (question_id) REFERENCES question (id)
);

CREATE TABLE exam (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    publisher_id BIGINT NOT NULL,
    paper_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    instructions VARCHAR(5000),
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    duration_minutes INT NOT NULL,
    passing_score DECIMAL(8, 2) NOT NULL,
    show_result_immediately BOOLEAN NOT NULL DEFAULT FALSE,
    show_answer_after_finish BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP,
    finished_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_test_exam_publisher FOREIGN KEY (publisher_id) REFERENCES `user` (id),
    CONSTRAINT fk_test_exam_paper FOREIGN KEY (paper_id) REFERENCES exam_paper (id)
);

CREATE TABLE exam_candidate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ASSIGNED',
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    submitted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_test_exam_candidate UNIQUE (exam_id, user_id),
    CONSTRAINT fk_test_candidate_exam FOREIGN KEY (exam_id) REFERENCES exam (id),
    CONSTRAINT fk_test_candidate_user FOREIGN KEY (user_id) REFERENCES `user` (id)
);

CREATE TABLE exam_attempt (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    attempt_no INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP NOT NULL,
    deadline_at TIMESTAMP NOT NULL,
    last_saved_at TIMESTAMP,
    submitted_at TIMESTAMP,
    submission_type VARCHAR(32),
    objective_score DECIMAL(8, 2),
    subjective_score DECIMAL(8, 2),
    final_score DECIMAL(8, 2),
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_test_attempt_exam_user_no UNIQUE (exam_id, user_id, attempt_no),
    CONSTRAINT fk_test_attempt_exam FOREIGN KEY (exam_id) REFERENCES exam (id),
    CONSTRAINT fk_test_attempt_candidate FOREIGN KEY (candidate_id) REFERENCES exam_candidate (id),
    CONSTRAINT fk_test_attempt_user FOREIGN KEY (user_id) REFERENCES `user` (id)
);

CREATE TABLE exam_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id BIGINT NOT NULL,
    paper_question_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_json VARCHAR(20000),
    answer_text VARCHAR(20000),
    max_score DECIMAL(8, 2) NOT NULL,
    score DECIMAL(8, 2),
    is_correct BOOLEAN,
    grading_status VARCHAR(32) NOT NULL DEFAULT 'UNANSWERED',
    grader_id BIGINT,
    grader_comment VARCHAR(2000),
    saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    graded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_test_answer_attempt_question UNIQUE (attempt_id, question_id),
    CONSTRAINT fk_test_answer_attempt FOREIGN KEY (attempt_id) REFERENCES exam_attempt (id),
    CONSTRAINT fk_test_answer_paper_question FOREIGN KEY (paper_question_id) REFERENCES exam_paper_question (id),
    CONSTRAINT fk_test_answer_question FOREIGN KEY (question_id) REFERENCES question (id)
);

CREATE TABLE exam_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    attempt_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    total_score DECIMAL(8, 2) NOT NULL,
    passing_score DECIMAL(8, 2) NOT NULL,
    passed BOOLEAN NOT NULL,
    correct_count INT NOT NULL DEFAULT 0,
    incorrect_count INT NOT NULL DEFAULT 0,
    unanswered_count INT NOT NULL DEFAULT 0,
    grading_completed BOOLEAN NOT NULL DEFAULT FALSE,
    visible_to_candidate BOOLEAN NOT NULL DEFAULT FALSE,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_test_result_attempt UNIQUE (attempt_id),
    CONSTRAINT fk_test_result_exam FOREIGN KEY (exam_id) REFERENCES exam (id),
    CONSTRAINT fk_test_result_attempt FOREIGN KEY (attempt_id) REFERENCES exam_attempt (id),
    CONSTRAINT fk_test_result_user FOREIGN KEY (user_id) REFERENCES `user` (id)
);

CREATE TABLE user_entitlement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    entitlement_type VARCHAR(32) NOT NULL,
    resource_id BIGINT,
    source_order_item_id BIGINT,
    total_quantity INT,
    available_quantity INT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    effective_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_entitlement_user FOREIGN KEY (user_id) REFERENCES `user` (id)
);
