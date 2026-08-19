CREATE TABLE IF NOT EXISTS ai_settings (
    id SMALLINT PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    model VARCHAR(120),
    max_output_tokens INTEGER NOT NULL DEFAULT 700,
    monthly_request_limit INTEGER,
    monthly_token_limit BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO ai_settings (id, enabled, max_output_tokens)
VALUES (1, FALSE, 700)
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS ai_usage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    provider VARCHAR(40) NOT NULL,
    model VARCHAR(120),
    input_tokens INTEGER NOT NULL DEFAULT 0,
    output_tokens INTEGER NOT NULL DEFAULT 0,
    total_tokens INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ai_usage_created_at ON ai_usage(created_at);
