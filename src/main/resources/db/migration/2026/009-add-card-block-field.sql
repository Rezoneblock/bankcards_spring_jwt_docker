ALTER TABLE cards
    ADD COLUMN block_reason VARCHAR(255);

ALTER TABLE cards
    ADD COLUMN blocked_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE cards
    ADD COLUMN blocked_by UUID;

ALTER TABLE cards
    ADD CONSTRAINT fk_card_blocked_by FOREIGN KEY (blocked_by) REFERENCES users(id);