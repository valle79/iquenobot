-- Group WhatsApp conversations support: marca conversaciones de grupos (@g.us)

ALTER TABLE conversations ADD COLUMN IF NOT EXISTS is_group BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_conversations_is_group ON conversations(is_group) WHERE is_deleted = FALSE;
