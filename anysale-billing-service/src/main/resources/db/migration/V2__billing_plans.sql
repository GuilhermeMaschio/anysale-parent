CREATE TABLE billing_plan (
    code VARCHAR(64) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(300) NOT NULL,
    monthly_price_cents INTEGER,
    user_limit INTEGER,
    monthly_lead_limit INTEGER,
    monthly_ai_request_limit INTEGER,
    trial_days INTEGER NOT NULL DEFAULT 14,
    grace_days INTEGER NOT NULL DEFAULT 5,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO billing_plan (code, name, description, monthly_price_cents, user_limit, monthly_lead_limit, monthly_ai_request_limit, trial_days, grace_days) VALUES
    ('ESSENTIAL', 'Essencial', 'Para começar uma operação comercial organizada.', 14900, 3, 1000, 500, 14, 5),
    ('PROFESSIONAL', 'Profissional', 'Para equipes que usam cadências, WhatsApp e IA no dia a dia.', 34900, 10, 5000, 3000, 14, 5),
    ('ENTERPRISE', 'Enterprise', 'Para operações com necessidades e limites personalizados.', NULL, NULL, NULL, NULL, 14, 5);
