-- Unique constraints on tenant identity fields + role limits for tenant admins
-- Company name and website must be unique across companies (soft-deleted excluded from unique index to allow reuse after deletion)
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS max_agents INT;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS max_supervisors INT;

CREATE UNIQUE INDEX IF NOT EXISTS uq_tenants_company_name
    ON tenants(company_name) WHERE is_deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_tenants_website_url
    ON tenants(website_url) WHERE website_url IS NOT NULL AND is_deleted = FALSE;
