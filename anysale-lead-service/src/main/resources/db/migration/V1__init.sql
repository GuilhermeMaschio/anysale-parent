

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS lead (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(120) NOT NULL,
    email VARCHAR(180),
    phone VARCHAR(40),
    source VARCHAR(80),
    desired_category VARCHAR(80),
    stage VARCHAR(30) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

-- Coleção de tags (ElementCollection)
CREATE TABLE IF NOT EXISTS lead_desired_tag  (
    lead_id UUID NOT NULL REFERENCES lead(id) ON DELETE CASCADE,
    tag VARCHAR(64) NOT NULL,
    PRIMARY KEY (lead_id, tag)
    );

CREATE TABLE IF NOT EXISTS lead_suggestion (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    lead_id UUID NOT NULL REFERENCES lead(id) ON DELETE CASCADE,
    product_id VARCHAR(80) NOT NULL,
    title VARCHAR(200) NOT NULL,
    price NUMERIC(12,2),
    currency VARCHAR(8),
    vendor VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

-- Índices úteis
CREATE INDEX IF NOT EXISTS idx_lead_stage ON lead(stage);
CREATE INDEX IF NOT EXISTS idx_lead_desired_tag_tag ON lead_desired_tag(tag);
