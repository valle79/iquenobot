ALTER TABLE knowledge_base ADD COLUMN extracted_text TEXT;

DROP INDEX IF EXISTS idx_kb_search;

CREATE INDEX idx_kb_search ON knowledge_base
    USING gin(to_tsvector('spanish',
        coalesce(content, '') || ' ' ||
        coalesce(extracted_text, '') || ' ' ||
        coalesce(title, '')));
