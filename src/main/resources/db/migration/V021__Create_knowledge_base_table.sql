CREATE TABLE knowledge_base (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    source_type VARCHAR(20) NOT NULL DEFAULT 'manual',
    source_url VARCHAR(1000),
    file_url VARCHAR(500),
    tags VARCHAR(500),

    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_kb_tenant ON knowledge_base(tenant_id, is_deleted);
CREATE INDEX idx_kb_search ON knowledge_base USING gin(to_tsvector('spanish', coalesce(content, '') || ' ' || coalesce(title, '')));

ALTER TABLE knowledge_base ADD CONSTRAINT fk_kb_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
