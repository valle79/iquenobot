-- ============================================
-- Control de respuestas automáticas:
-- agregación (debounce), anti-duplicados y anti-spam
-- ============================================

-- Conversation: estado de respuesta de IA pendiente + controles anti-duplicados
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS pending_ai_response BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS last_bot_response_at TIMESTAMP;
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS last_processed_message_hash VARCHAR(64);

-- Message: marca si el mensaje ya fue consumido por la consolidación
ALTER TABLE conversation_messages ADD COLUMN IF NOT EXISTS ai_processed BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill: los mensajes existentes (respondidos por el flujo síncrono anterior)
-- ya fueron consumidos; solo los mensajes nuevos pasan por la consolidación.
UPDATE conversation_messages SET ai_processed = TRUE
WHERE direction = 'INBOUND' AND ai_processed = FALSE;

-- Índice para el scheduler de consolidación (conversaciones pendientes por antigüedad)
CREATE INDEX IF NOT EXISTS idx_conversations_pending_ai_response
    ON conversations(pending_ai_response, last_message_at)
    WHERE is_deleted = FALSE;

-- Índice para localizar mensajes entrantes no procesados de una conversación
CREATE INDEX IF NOT EXISTS idx_conversation_messages_pending
    ON conversation_messages(conversation_id, direction, ai_processed)
    WHERE direction = 'INBOUND' AND ai_processed = FALSE;
