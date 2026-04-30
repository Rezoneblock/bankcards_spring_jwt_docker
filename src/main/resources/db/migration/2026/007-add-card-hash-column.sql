ALTER TABLE cards
ADD COLUMN card_hash_number VARCHAR(64) NOT NULL DEFAULT 'temp_hash_value' NOT NULL;

ALTER TABLE cards
    ALTER COLUMN card_hash_number DROP DEFAULT;

ALTER TABLE cards
ADD CONSTRAINT uk_cards_card_hash_number UNIQUE (card_hash_number);

CREATE INDEX idx_cards_card_hash_number ON cards(card_hash_number);

COMMENT ON COLUMN cards.card_hash_number IS 'SHA-256 хеш номера карты для поиска дубликатов';