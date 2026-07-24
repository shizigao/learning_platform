CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(64) NOT NULL UNIQUE,
    product_type VARCHAR(32) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    resource_id BIGINT,
    quantity INT,
    price DECIMAL(10, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_PAYMENT',
    total_amount DECIMAL(10, 2) NOT NULL,
    payable_amount DECIMAL(10, 2) NOT NULL,
    paid_amount DECIMAL(10, 2),
    payment_method VARCHAR(32),
    remark VARCHAR(500),
    expires_at TIMESTAMP,
    paid_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_order_user FOREIGN KEY (user_id) REFERENCES `user` (id)
);

CREATE TABLE order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT,
    product_code_snapshot VARCHAR(64) NOT NULL,
    product_type_snapshot VARCHAR(32) NOT NULL,
    product_name_snapshot VARCHAR(200) NOT NULL,
    resource_id_snapshot BIGINT,
    unit_price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    entitlement_quantity INT,
    subtotal_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_item_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_test_item_product FOREIGN KEY (product_id) REFERENCES product (id)
);

CREATE TABLE payment_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_no VARCHAR(64) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL DEFAULT 'MOCK',
    provider_transaction_no VARCHAR(128),
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    request_payload VARCHAR(4000),
    response_payload VARCHAR(4000),
    failure_reason VARCHAR(1000),
    paid_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_test_payment_provider_txn UNIQUE (provider, provider_transaction_no),
    CONSTRAINT fk_test_payment_order FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE TABLE user_entitlement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    entitlement_type VARCHAR(32) NOT NULL,
    resource_id BIGINT,
    source_order_item_id BIGINT UNIQUE,
    total_quantity INT,
    available_quantity INT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    effective_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_order_entitlement_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_test_order_entitlement_item FOREIGN KEY (source_order_item_id) REFERENCES order_item (id)
);
