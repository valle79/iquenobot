-- Create conversation_messages table
CREATE TABLE conversation_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    conversation_id UUID NOT NULL,
    user_id UUID,
    direction VARCHAR(10) NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    content TEXT,
    channel_message_id VARCHAR(100),
    reply_to_message_id VARCHAR(100),
    sender_name VARCHAR(100),
    sender_phone VARCHAR(20),
    sender_email VARCHAR(255),
    is_from_bot BOOLEAN NOT NULL DEFAULT FALSE,
    bot_intent VARCHAR(100),
    bot_confidence REAL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    failed_at TIMESTAMP,
    failure_reason VARCHAR(500),
    metadata TEXT,
    
    -- Base entity fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes
CREATE INDEX idx_conversation_messages_tenant_id ON conversation_messages(tenant_id);
CREATE INDEX idx_conversation_messages_conversation_id ON conversation_messages(conversation_id);
CREATE INDEX idx_conversation_messages_user_id ON conversation_messages(user_id);
CREATE INDEX idx_conversation_messages_direction ON conversation_messages(direction);
CREATE INDEX idx_conversation_messages_type ON conversation_messages(type);
CREATE INDEX idx_conversation_messages_status ON conversation_messages(status);
CREATE INDEX idx_conversation_messages_sent_at ON conversation_messages(sent_at DESC);
CREATE INDEX idx_conversation_messages_created_at ON conversation_messages(created_at DESC);
CREATE INDEX idx_conversation_messages_channel_message_id ON conversation_messages(channel_message_id) WHERE channel_message_id IS NOT NULL;
CREATE INDEX idx_conversation_messages_is_from_bot ON conversation_messages(is_from_bot);

-- Create composite indexes for common queries
CREATE INDEX idx_conversation_messages_conv_sent ON conversation_messages(conversation_id, sent_at DESC);
CREATE INDEX idx_conversation_messages_tenant_created ON conversation_messages(tenant_id, created_at DESC);
CREATE INDEX idx_conversation_messages_unread ON conversation_messages(conversation_id, status) WHERE status = 'DELIVERED';

-- Add constraints
ALTER TABLE conversation_messages ADD CONSTRAINT check_conversation_messages_direction 
    CHECK (direction IN ('INBOUND', 'OUTBOUND'));

ALTER TABLE conversation_messages ADD CONSTRAINT check_conversation_messages_type 
    CHECK (type IN ('TEXT', 'IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT', 'LOCATION', 'CONTACT', 'STICKER', 'TEMPLATE', 'INTERACTIVE', 'SYSTEM'));

ALTER TABLE conversation_messages ADD CONSTRAINT check_conversation_messages_status 
    CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED', 'DELETED'));

-- Foreign key constraints
ALTER TABLE conversation_messages ADD CONSTRAINT fk_conversation_messages_tenant_id 
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

ALTER TABLE conversation_messages ADD CONSTRAINT fk_conversation_messages_conversation_id 
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE;

ALTER TABLE conversation_messages ADD CONSTRAINT fk_conversation_messages_user_id 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

-- Create trigger for updated_at
CREATE TRIGGER update_conversation_messages_updated_at BEFORE UPDATE ON conversation_messages 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();