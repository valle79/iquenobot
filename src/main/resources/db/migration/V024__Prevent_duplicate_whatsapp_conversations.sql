-- Garantiza una sola conversación activa por tenant + canal + channel_conversation_id
-- Esto evita duplicados cuando Evolution API envía instanceId inconsistente

-- 1. Limpiar duplicados: migrar mensajes a la conversación más antigua y soft-delete las duplicadas
WITH duplicados AS (
    SELECT tenant_id, channel, channel_conversation_id
    FROM conversations
    WHERE is_deleted = false
      AND channel = 'WHATSAPP'
      AND channel_conversation_id IS NOT NULL
    GROUP BY tenant_id, channel, channel_conversation_id
    HAVING COUNT(*) > 1
),
buenas AS (
    SELECT DISTINCT ON (c.tenant_id, c.channel, c.channel_conversation_id)
        c.id AS keep_id,
        c.tenant_id,
        c.channel,
        c.channel_conversation_id
    FROM conversations c
    INNER JOIN duplicados d
        ON  c.tenant_id = d.tenant_id
        AND c.channel = d.channel
        AND c.channel_conversation_id = d.channel_conversation_id
    WHERE c.is_deleted = false
    ORDER BY c.tenant_id, c.channel, c.channel_conversation_id, c.created_at ASC
)
UPDATE conversation_messages m
SET conversation_id = b.keep_id
FROM buenas b
INNER JOIN conversations c
    ON  c.tenant_id = b.tenant_id
    AND c.channel = b.channel
    AND c.channel_conversation_id = b.channel_conversation_id
    AND c.id != b.keep_id
    AND c.is_deleted = false
WHERE m.conversation_id = c.id;

-- 2. Soft-delete conversaciones duplicadas (excepto la más antigua)
WITH duplicados AS (
    SELECT tenant_id, channel, channel_conversation_id
    FROM conversations
    WHERE is_deleted = false
      AND channel = 'WHATSAPP'
      AND channel_conversation_id IS NOT NULL
    GROUP BY tenant_id, channel, channel_conversation_id
    HAVING COUNT(*) > 1
),
to_delete AS (
    SELECT c.id
    FROM conversations c
    INNER JOIN duplicados d
        ON  c.tenant_id = d.tenant_id
        AND c.channel = d.channel
        AND c.channel_conversation_id = d.channel_conversation_id
    WHERE c.is_deleted = false
      AND c.id NOT IN (
          SELECT DISTINCT ON (c2.tenant_id, c2.channel, c2.channel_conversation_id)
              c2.id
          FROM conversations c2
          INNER JOIN duplicados d2
              ON  c2.tenant_id = d2.tenant_id
              AND c2.channel = d2.channel
              AND c2.channel_conversation_id = d2.channel_conversation_id
          WHERE c2.is_deleted = false
          ORDER BY c2.tenant_id, c2.channel, c2.channel_conversation_id, c2.created_at ASC
      )
)
UPDATE conversations
SET is_deleted = true,
    deleted_at = NOW()
WHERE id IN (SELECT id FROM to_delete);

-- 3. Índice único para prevenir duplicados futuros
CREATE UNIQUE INDEX IF NOT EXISTS uk_conversations_tenant_channel_external
ON conversations (tenant_id, channel, channel_conversation_id)
WHERE is_deleted = false;
