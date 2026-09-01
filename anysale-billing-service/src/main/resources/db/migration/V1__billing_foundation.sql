CREATE TABLE IF NOT EXISTS billing_subscription (
    tenant_id VARCHAR(64) PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    provider_customer_id VARCHAR(128),
    provider_subscription_id VARCHAR(128),
    plan_code VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    trial_ends_at TIMESTAMPTZ,
    current_period_ends_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_billing_subscription_provider_reference UNIQUE (provider, provider_subscription_id)
);

-- This table existed in early local environments while Billing was still co-located with Lead Service.
-- Keep the migration compatible so ownership can move to Billing without deleting webhook history.
CREATE TABLE IF NOT EXISTS billing_webhook_event (
    id UUID PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    provider_event_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(96) NOT NULL,
    payload JSONB NOT NULL,
    processing_result VARCHAR(32) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    CONSTRAINT uk_billing_webhook_provider_event UNIQUE (provider, provider_event_id)
);
