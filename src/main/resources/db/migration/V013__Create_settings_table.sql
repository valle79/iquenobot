-- Create settings table
CREATE TABLE settings (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    category VARCHAR(50) NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    setting_type VARCHAR(20),
    description VARCHAR(500),

    -- Soft delete fields
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    deleted_by UUID,

    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes
CREATE INDEX idx_settings_tenant_id ON settings(tenant_id);
CREATE INDEX idx_settings_category ON settings(category);
CREATE INDEX idx_settings_tenant_category ON settings(tenant_id, category);
CREATE UNIQUE INDEX idx_settings_tenant_category_key ON settings(tenant_id, category, setting_key) WHERE is_deleted = FALSE;

-- Foreign key
ALTER TABLE settings ADD CONSTRAINT fk_settings_tenant_id
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
