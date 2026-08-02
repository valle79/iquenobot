-- ============================================
-- WhatsApp Channels: múltiples números por tenant
-- una sola Evolution API, resolución por instance_name
-- ============================================

-- 1. Eliminar versión previa (intento anterior aplicado con esquema distinto,
--    tabla vacía: 0 filas). Se reconstruye con el esquema definitivo.
DROP TABLE IF EXISTS whatsapp_channels;

-- 2. Crear tabla definitiva
CREATE TABLE whatsapp_channels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    channel_name VARCHAR(100) NOT NULL,
    instance_name VARCHAR(120) NOT NULL UNIQUE,
    phone_number VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'DISCONNECTED',
    webhook_url VARCHAR(255),
    connected_at TIMESTAMP,

    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,

    -- Soft delete fields
    deleted_at TIMESTAMP,
    deleted_by UUID,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 3. Índices
CREATE INDEX idx_whatsapp_channels_tenant_id ON whatsapp_channels(tenant_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_whatsapp_channels_instance_name ON whatsapp_channels(instance_name) WHERE is_deleted = FALSE;
CREATE INDEX idx_whatsapp_channels_status ON whatsapp_channels(status) WHERE is_deleted = FALSE;

-- 4. Constraints
ALTER TABLE whatsapp_channels ADD CONSTRAINT check_whatsapp_channels_status
    CHECK (status IN ('CONNECTED', 'DISCONNECTED', 'CONNECTING', 'ERROR', 'REVOKED'));

-- 5. Foreign key
ALTER TABLE whatsapp_channels ADD CONSTRAINT fk_whatsapp_channels_tenant_id
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

-- 6. Trigger para updated_at
CREATE TRIGGER update_whatsapp_channels_updated_at BEFORE UPDATE ON whatsapp_channels
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 7. Migración de datos: convertir los settings de whatsapp existentes
--    (instance_id, phone_number, webhook_url, connected) en canales.
INSERT INTO whatsapp_channels (
    id, tenant_id, channel_name, instance_name, phone_number, status,
    webhook_url, connected_at, created_at, updated_at, version, is_deleted
)
SELECT
    gen_random_uuid(),
    s.tenant_id,
    'WhatsApp Principal',
    s.setting_value,
    p.setting_value,
    CASE WHEN c.setting_value = 'true' THEN 'CONNECTED' ELSE 'DISCONNECTED' END,
    w.setting_value,
    CASE WHEN c.setting_value = 'true' THEN s.created_at ELSE NULL END,
    s.created_at,
    s.updated_at,
    0,
    FALSE
FROM settings s
LEFT JOIN settings p ON p.tenant_id = s.tenant_id AND p.category = 'whatsapp' AND p.setting_key = 'phone_number' AND p.is_deleted = FALSE
LEFT JOIN settings w ON w.tenant_id = s.tenant_id AND w.category = 'whatsapp' AND w.setting_key = 'webhook_url' AND w.is_deleted = FALSE
LEFT JOIN settings c ON c.tenant_id = s.tenant_id AND c.category = 'whatsapp' AND c.setting_key = 'connected' AND c.is_deleted = FALSE
WHERE s.category = 'whatsapp' AND s.setting_key = 'instance_id' AND s.is_deleted = FALSE
ON CONFLICT (instance_name) DO NOTHING;
