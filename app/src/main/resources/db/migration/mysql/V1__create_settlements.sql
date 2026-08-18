CREATE TABLE settlements (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    trace_id VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_settlements_event_id UNIQUE (event_id),
    CONSTRAINT uk_settlements_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT uk_settlements_order_id UNIQUE (order_id)
);
