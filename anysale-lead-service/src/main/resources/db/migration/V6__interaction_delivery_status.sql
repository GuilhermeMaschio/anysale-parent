ALTER TABLE interaction
    ADD COLUMN IF NOT EXISTS delivery_status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS delivery_status_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delivery_recipient_id VARCHAR(40),
    ADD COLUMN IF NOT EXISTS delivery_error_code VARCHAR(120),
    ADD COLUMN IF NOT EXISTS delivery_error_title VARCHAR(255),
    ADD COLUMN IF NOT EXISTS delivery_error_message VARCHAR(2000);
