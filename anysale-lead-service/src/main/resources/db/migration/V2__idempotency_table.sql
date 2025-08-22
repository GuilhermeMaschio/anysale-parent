CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS
    idempotency_record (
                      id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                      operation        VARCHAR(100) NOT NULL,
                      resource_id      UUID NULL,
                      idempotency_key  VARCHAR(128) NOT NULL,
                      request_hash     VARCHAR(64) NOT NULL,   -- SHA-256 em hex (64 chars)
                      status_code      INT NOT NULL,
                      content_type     VARCHAR(100),
                      etag             VARCHAR(100),
                      location         TEXT,
                      response_body    TEXT,                   -- JSON da resposta
                      created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
                      expires_at       TIMESTAMPTZ NULL
);

-- evita reaplicar mesma chave/operacao/recurso
CREATE UNIQUE INDEX IF NOT EXISTS uk_idem
    ON idempotency_record(operation, resource_id, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_idem_exp
    ON idempotency_record(expires_at);