ALTER TABLE card_block_requests
    ADD COLUMN reject_reason VARCHAR(255);

COMMENT ON COLUMN card_block_requests.reject_reason IS 'Причина отклонения заявки администратором';