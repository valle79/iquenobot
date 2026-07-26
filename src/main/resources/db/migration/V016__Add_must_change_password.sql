ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_tenants_expired ON tenants(subscription_expires_at) WHERE is_deleted = FALSE AND status = 'ACTIVE';
