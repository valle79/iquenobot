-- Human handoff: pausa temporal del bot cuando un agente humano interviene
-- en la conversación, con recuperación automática tras inactividad del agente.

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS human_handoff BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS human_taken_over_at TIMESTAMP;

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS last_agent_reply_at TIMESTAMP;

ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS bot_resume_after TIMESTAMP;

-- Registros antiguos: sin handoff (NULL se trata como FALSE en el servicio).
-- No se requiere backfill: el DEFAULT FALSE ya cubre las filas existentes.
