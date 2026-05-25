ALTER TABLE lead
    ADD COLUMN IF NOT EXISTS last_message VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS last_interaction_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_lead_phone ON lead(phone);

CREATE TABLE IF NOT EXISTS interaction (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    lead_id UUID NOT NULL REFERENCES lead(id) ON DELETE CASCADE,
    message VARCHAR(2000) NOT NULL,
    channel VARCHAR(40) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    external_message_id VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_interaction_lead_id ON interaction(lead_id);
CREATE INDEX IF NOT EXISTS idx_interaction_created_at ON interaction(created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_interaction_channel_external_message
    ON interaction(channel, external_message_id)
    WHERE external_message_id IS NOT NULL;
