CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

CREATE INDEX idx_cards_user_id ON cards(user_id);
CREATE INDEX idx_cards_card_number ON cards(card_number);