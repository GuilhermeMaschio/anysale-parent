ALTER TABLE lead
    ADD COLUMN IF NOT EXISTS assigned_to VARCHAR(120),
    ADD COLUMN IF NOT EXISTS estimated_value NUMERIC(14,2),
    ADD COLUMN IF NOT EXISTS actual_value NUMERIC(14,2),
    ADD COLUMN IF NOT EXISTS lost_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS lead_stage_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), lead_id UUID NOT NULL REFERENCES lead(id) ON DELETE CASCADE,
    from_stage VARCHAR(30), to_stage VARCHAR(30) NOT NULL, changed_by VARCHAR(120), reason VARCHAR(500), created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_lead_stage_history_lead_created ON lead_stage_history(lead_id, created_at);
