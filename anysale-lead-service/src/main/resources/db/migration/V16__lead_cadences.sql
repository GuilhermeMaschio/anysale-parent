CREATE TABLE sales_playbook_step (
    id UUID PRIMARY KEY,
    playbook_id UUID NOT NULL REFERENCES sales_playbook(id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK (position > 0),
    delay_minutes INTEGER NOT NULL CHECK (delay_minutes >= 0),
    title VARCHAR(240) NOT NULL,
    task_type VARCHAR(40) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    note VARCHAR(1000),
    UNIQUE (playbook_id, position)
);

CREATE TABLE lead_cadence (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenant(id),
    lead_id UUID NOT NULL UNIQUE REFERENCES lead(id) ON DELETE CASCADE,
    playbook_id UUID NOT NULL REFERENCES sales_playbook(id),
    status VARCHAR(20) NOT NULL,
    next_position INTEGER NOT NULL,
    next_action_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ NOT NULL,
    paused_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lead_cadence_due ON lead_cadence(status, next_action_at) WHERE status = 'ACTIVE';
