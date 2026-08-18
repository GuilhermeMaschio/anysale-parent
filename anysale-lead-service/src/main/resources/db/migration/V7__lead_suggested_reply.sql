ALTER TABLE lead
    ADD COLUMN IF NOT EXISTS suggested_reply VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS suggested_reply_generated_at TIMESTAMPTZ;
