CREATE TABLE lead_task (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenant(id),
    lead_id UUID NOT NULL REFERENCES lead(id),
    title VARCHAR(240) NOT NULL,
    task_type VARCHAR(40) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    due_at TIMESTAMPTZ NOT NULL,
    assigned_to VARCHAR(128),
    reserved_at TIMESTAMPTZ,
    reservation_expires_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    outcome VARCHAR(40),
    note VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_lead_task_queue ON lead_task(tenant_id, status, due_at, priority);
CREATE INDEX idx_lead_task_assignee ON lead_task(tenant_id, assigned_to, status, due_at);
CREATE INDEX idx_lead_task_lead ON lead_task(lead_id, created_at DESC);
