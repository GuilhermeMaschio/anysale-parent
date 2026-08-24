CREATE TABLE console_preference (
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenant(id),
    user_id VARCHAR(128) NOT NULL,
    color_theme VARCHAR(16) NOT NULL DEFAULT 'light',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, user_id),
    CONSTRAINT chk_console_preference_theme CHECK (color_theme IN ('light', 'dark'))
);
