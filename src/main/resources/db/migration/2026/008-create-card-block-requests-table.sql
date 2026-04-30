CREATE TABLE card_block_requests (
    id BIGSERIAL PRIMARY KEY,
    card_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    processed_by UUID,
    CONSTRAINT fk_block_request_card FOREIGN KEY (card_id) REFERENCES cards(id),
    CONSTRAINT fk_block_request_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_block_request_processed_by FOREIGN KEY (processed_by) REFERENCES users(id)
);

CREATE INDEX idx_block_requests_card_id ON card_block_requests(card_id);
CREATE INDEX idx_block_requests_user_id ON card_block_requests(user_id);
CREATE INDEX idx_block_requests_status ON card_block_requests(status);

COMMENT ON TABLE card_block_requests IS 'Запросы пользователей на блокировку карт';
COMMENT ON COLUMN card_block_requests.status IS 'PENDING, APPROVED, REJECTED';