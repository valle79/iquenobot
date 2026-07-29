-- Add normalized_phone column for canonical phone format (e.g. +51923189009)
ALTER TABLE contacts ADD COLUMN normalized_phone VARCHAR(20);

-- Helper: extract only digits from phone
CREATE OR REPLACE FUNCTION temp_normalize_phone(p_phone TEXT)
RETURNS TEXT AS $$
DECLARE
    digits TEXT;
BEGIN
    IF p_phone IS NULL OR p_phone = '' THEN
        RETURN NULL;
    END IF;
    digits := regexp_replace(p_phone, '[^0-9]', '', 'g');
    IF digits = '' THEN
        RETURN NULL;
    END IF;
    IF length(digits) = 9 AND digits ~ '^9[0-9]{8}$' THEN
        RETURN '+51' || digits;
    END IF;
    IF length(digits) = 11 AND digits ~ '^51[0-9]{9}$' THEN
        RETURN '+' || digits;
    END IF;
    IF p_phone ~ '^\+' THEN
        RETURN '+' || digits;
    END IF;
    RETURN '+' || digits;
END;
$$ LANGUAGE plpgsql;

-- Populate normalized_phone for existing contacts
UPDATE contacts
SET normalized_phone = temp_normalize_phone(phone)
WHERE phone IS NOT NULL AND phone != '';

-- Fill remaining from whatsapp_phone
UPDATE contacts
SET normalized_phone = temp_normalize_phone(whatsapp_phone)
WHERE (normalized_phone IS NULL OR normalized_phone = '')
  AND whatsapp_phone IS NOT NULL AND whatsapp_phone != '';

-- Drop the helper function
DROP FUNCTION IF EXISTS temp_normalize_phone;

-- Remove duplicates: keep the contact with most filled fields per (tenant_id, normalized_phone)
DELETE FROM contacts
WHERE id NOT IN (
    SELECT DISTINCT ON (tenant_id, normalized_phone) id
    FROM contacts
    WHERE normalized_phone IS NOT NULL
      AND is_deleted = FALSE
    ORDER BY
        tenant_id,
        normalized_phone,
        (CASE WHEN full_name IS NOT NULL AND full_name != '' THEN 1 ELSE 0 END +
         CASE WHEN email IS NOT NULL AND email != '' THEN 1 ELSE 0 END +
         CASE WHEN first_name IS NOT NULL AND first_name != '' THEN 1 ELSE 0 END +
         CASE WHEN last_name IS NOT NULL AND last_name != '' THEN 1 ELSE 0 END +
         CASE WHEN company IS NOT NULL AND company != '' THEN 1 ELSE 0 END +
         CASE WHEN phone IS NOT NULL AND phone != '' THEN 1 ELSE 0 END) DESC,
        created_at ASC
)
AND is_deleted = FALSE;

-- Create unique index on (tenant_id, normalized_phone) to prevent duplicates
CREATE UNIQUE INDEX uk_contacts_tenant_normalized_phone
ON contacts (tenant_id, normalized_phone)
WHERE is_deleted = FALSE AND normalized_phone IS NOT NULL;

-- Create index for lookups
CREATE INDEX idx_contacts_normalized_phone
ON contacts (normalized_phone)
WHERE is_deleted = FALSE AND normalized_phone IS NOT NULL;
