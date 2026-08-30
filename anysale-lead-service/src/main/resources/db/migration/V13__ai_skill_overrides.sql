CREATE TABLE ai_skill_override (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenant(id),
    profile VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_ai_skill_override_tenant_profile UNIQUE (tenant_id, profile)
);
