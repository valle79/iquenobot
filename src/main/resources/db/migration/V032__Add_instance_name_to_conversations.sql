-- WhatsApp channel resolution para mensajes salientes:
-- cada conversación recuerda el instance_name (canal) que la originó,
-- permitiendo múltiples números por tenant.

ALTER TABLE conversations ADD COLUMN IF NOT EXISTS instance_name VARCHAR(120);

-- Backfill: conversaciones WhatsApp existentes apuntan al canal más antiguo
-- del tenant (el migrado de settings como "WhatsApp Principal").
UPDATE conversations c
SET instance_name = wc.instance_name
FROM (
    SELECT DISTINCT ON (tenant_id) tenant_id, instance_name
    FROM whatsapp_channels
    WHERE is_deleted = FALSE
    ORDER BY tenant_id, created_at ASC
) wc
WHERE c.channel = 'WHATSAPP'
  AND c.tenant_id = wc.tenant_id
  AND c.is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_conversations_instance_name ON conversations(instance_name) WHERE is_deleted = FALSE;
