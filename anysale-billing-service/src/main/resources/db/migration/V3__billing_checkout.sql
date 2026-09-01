CREATE TABLE billing_checkout (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    plan_code VARCHAR(64) NOT NULL REFERENCES billing_plan(code),
    provider VARCHAR(32) NOT NULL,
    provider_checkout_id VARCHAR(128) NOT NULL,
    external_reference VARCHAR(200) NOT NULL,
    checkout_url VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_billing_checkout_provider_id UNIQUE (provider, provider_checkout_id),
    CONSTRAINT uk_billing_checkout_external_reference UNIQUE (external_reference)
);
CREATE INDEX idx_billing_checkout_tenant_created ON billing_checkout(tenant_id, created_at DESC);
