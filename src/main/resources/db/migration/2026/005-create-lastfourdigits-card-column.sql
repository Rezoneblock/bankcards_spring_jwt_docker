ALTER TABLE cards
ADD COLUMN last_four_digits VARCHAR(4);

CREATE INDEX idx_cards_last_four_digits ON cards(last_four_digits);