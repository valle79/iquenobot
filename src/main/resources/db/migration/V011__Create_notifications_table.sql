-- Create notifications table
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    action_url VARCHAR(500),
    action_label VARCHAR(100),
    icon VARCHAR(100),
    image_url VARCHAR(500),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    is_sent BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMP,
    is_delivered BOOLEAN NOT NULL DEFAULT FALSE,
    delivered_at TIMESTAMP,
    failed BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    scheduled_at TIMESTAMP,
    expires_at TIMESTAMP,
    metadata TEXT,
    related_entity_type VARCHAR(50),
    related_entity_id VARCHAR(255),
    
    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes
CREATE INDEX idx_notifications_tenant_id ON notifications(tenant_id);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_priority ON notifications(priority);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_is_sent ON notifications(is_sent);
CREATE INDEX idx_notifications_failed ON notifications(failed);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
CREATE INDEX idx_notifications_scheduled_at ON notifications(scheduled_at);
CREATE INDEX idx_notifications_expires_at ON notifications(expires_at);

-- Create composite indexes for common queries
CREATE INDEX idx_notifications_tenant_user ON notifications(tenant_id, user_id);
CREATE INDEX idx_notifications_tenant_user_read ON notifications(tenant_id, user_id, is_read);
CREATE INDEX idx_notifications_tenant_user_type ON notifications(tenant_id, user_id, type);
CREATE INDEX idx_notifications_user_priority_read ON notifications(user_id, priority, is_read);
CREATE INDEX idx_notifications_pending ON notifications(tenant_id, is_sent, failed, scheduled_at) 
    WHERE is_sent = FALSE AND failed = FALSE;

-- Add constraints
ALTER TABLE notifications ADD CONSTRAINT check_notifications_type 
    CHECK (type IN ('NEW_MESSAGE', 'NEW_CONVERSATION', 'CONVERSATION_ASSIGNED', 'LEAD_ASSIGNED', 
                    'LEAD_STATUS_CHANGED', 'TASK_REMINDER', 'SYSTEM_ALERT', 'MENTION', 
                    'LOW_STOCK_ALERT', 'NEW_ORDER', 'ORDER_STATUS_CHANGED', 'WELCOME', 
                    'ANNOUNCEMENT', 'CUSTOM'));

ALTER TABLE notifications ADD CONSTRAINT check_notifications_channel 
    CHECK (channel IN ('IN_APP', 'EMAIL', 'SMS', 'PUSH', 'WHATSAPP', 'WEBHOOK'));

ALTER TABLE notifications ADD CONSTRAINT check_notifications_priority 
    CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'));

ALTER TABLE notifications ADD CONSTRAINT check_notifications_retry_count 
    CHECK (retry_count >= 0 AND retry_count <= max_retries);

-- Foreign key constraints
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_tenant_id 
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

ALTER TABLE notifications ADD CONSTRAINT fk_notifications_user_id 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Create trigger for updated_at
CREATE TRIGGER update_notifications_updated_at BEFORE UPDATE ON notifications 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
